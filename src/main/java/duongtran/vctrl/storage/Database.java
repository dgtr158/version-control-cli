package duongtran.vctrl.storage;

import duongtran.vctrl.Workspace;
import duongtran.vctrl.concurrency.Lockfile;
import duongtran.vctrl.references.Refs;
import duongtran.vctrl.storage.objects.Blob;
import duongtran.vctrl.storage.objects.Commit;
import duongtran.vctrl.storage.objects.Tree;
import duongtran.vctrl.utils.DirectoryNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Responsible for storing content in .vctrl/objects
 */
public class Database {
    private static Database instance;
    private Path dbPath;

    private static final Logger logger = LoggerFactory.getLogger(Database.class);

    private static final String TEMP_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int TEMP_NAME_LENGTH = 6;
    private static final String TEMP_PREFIX = "tmp_obj_";
    private static final int BUFFER_SIZE = 8192; // Increased buffer size for better performance

    private Database() {
    }

    /**
     * Initializes the database instance by setting up the database path.
     * This method retrieves the virtual control (vctrl) path from the workspace
     * and resolves the database path to the objects directory. It ensures that
     * the singleton instance of the Database class has its internal state correctly
     * initialized with the path to the storage location.
     */
    public static void initialize() {
        Path vctrlPath = Workspace.getInstance().getVctrlPath();
        getInstance().dbPath = vctrlPath.resolve(DirectoryNames.OBJECTS);
    }

    /**
     * Provides access to the singleton instance of the Database class.
     * If the instance does not exist, it initializes and returns a new one.
     *
     * @return The singleton instance of the Database class.
     */
    public static Database getInstance() {
        if (instance == null) {
            instance = new Database();
        }
        return instance;
    }

    /**
     * Stores a blob object in the database.
     *
     * @param object The blob objects to store
     * @throws IOException              If failed to write the object
     * @throws IllegalArgumentException If an object is null
     */
    public ObjectID store(ObjectStorage object) throws IOException, NoSuchAlgorithmException {
        if (object == null) {
            throw new IllegalArgumentException("Blob object cannot be null");
        }
        byte[] content = object.toBytes();
        object.calculateOid(content);
        writeObject(object.getOid(), content);
        return object.getOid();
    }

    /**
     * Loads an object from the database, based on its identifier and type.
     * The method reads the object's data, decompresses it, performs deserialization,
     * and returns the corresponding object representation.
     *
     * @param objectID The identifier of the object to be loaded. It uniquely identifies the object in the database.
     * @param type     The type of the object being loaded. Determines how the data is deserialized.
     * @return The deserialized object corresponding to the specified identifier and type.
     * @throws IOException              If an I/O error occurs while accessing the object data.
     * @throws NoSuchAlgorithmException If the checksum verification process requires an unsupported algorithm.
     * @throws IllegalArgumentException If the specified type is not recognized.
     */
    public ObjectStorage loadObject(ObjectID objectID, ObjectType type) throws IOException, NoSuchAlgorithmException, IllegalArgumentException {

        if (objectID == null || objectID.isEmpty()) return null;
        Path path = constructObjectPath(objectID.getValue());
        byte[] bytes;
        try (Lockfile in = new Lockfile(path)) {
            in.acquire();
            byte[] compressedBytes = in.read(path);
            bytes = uncompressData(compressedBytes);

            // TODO: Verify checksum
        }

        // Deserialize the object
        ObjectStorage object = switch (type) {
            case BLOB -> Blob.fromBytes(bytes);
            case TREE -> Tree.fromBytes(bytes);
            case COMMIT -> Commit.fromBytes(bytes);
            default -> throw new IllegalArgumentException("Type cannot be: " + type);
        };
        object.setOid(objectID);
        return object;

    }


    /**
     * Writes a binary object with a specific object ID to the database. The method first
     * writes the compressed content to a temporary file, then atomically moves it to its final
     * location. If any error occurs during the process, the temporary file is cleaned up.
     *
     * @param oid     The object ID, typically a hashed value used as the filename.
     * @param content The byte array representing the content of the object to be stored.
     * @throws IOException If an I/O error occurs during directory creation, writing the content,
     *                     or moving the file to its final location.
     */
    private void writeObject(ObjectID oid, byte[] content) throws IOException {
        Path objectPath = constructObjectPath(oid.getValue());
        if (Files.exists(objectPath)) {
            return;
        }
        Path dirname = objectPath.getParent();
        Path tempPath = dirname.resolve(generateTempName());

        try {
            Files.createDirectories(dirname);
            writeCompressedContent(tempPath, content);
            moveToFinalLocation(tempPath, objectPath);
        } catch (IOException e) {
            cleanupTempFile(tempPath);
            logger.error("Failed to write object to database", e);
            throw e;
        }
    }

    /**
     * Constructs the path of an object in the database using its object ID (OID).
     * The method resolves the path based on the first two characters of the OID
     * as a directory, and the remaining characters as the file name under that
     * directory.
     *
     * @param oid The object ID, a hashed value typically used to identify and locate
     *            the object in the database.
     * @return A {@code Path} object representing the resolved location in the database
     * where the object resides or should reside.
     */
    public Path constructObjectPath(String oid) {
        return dbPath.resolve(oid.substring(0, 2))
                .resolve(oid.substring(2));
    }

    /**
     * Lists all files tracked in the repository's latest commit referred to by the HEAD pointer.
     * The method recursively traverses the file system structure represented by the commit's tree
     * and gathers all file entries into a map, where the keys are the file paths,
     * and the values are their corresponding {@code DataEntry} objects.
     *
     * @return A map containing file paths as keys and their corresponding {@code DataEntry} metadata as values.
     * @throws IOException              If an I/O error occurs during file traversal or retrieval of data.
     * @throws NoSuchAlgorithmException If a required cryptographic algorithm is not available.
     */
    public static Map<Path, DataEntry> listFileInHead() throws IOException, NoSuchAlgorithmException {
        Refs refs = new Refs();
        String headCommitID = refs.readHead();
        if (headCommitID == null) {
            return new TreeMap<>();
        }

        Map<Path, DataEntry> filesMap = new TreeMap<>();
        Commit commit = Commit.loadCommit(new ObjectID(headCommitID));
        Tree.listAllFiles(
                commit.getTreeOid()
                , Workspace.getInstance().getRootPath()
                , filesMap
        );
        return filesMap;
    }

    /**
     * Retrieves the content of a blob object identified by the given object ID as a list of lines.
     * The blob content is assumed to be UTF-8 encoded, and separated into lines based on line endings.
     * If reading the blob fails, an empty list is returned.
     *
     * @param objectID The identifier of the blob object to load. Must not be null.
     * @return A list of strings, where each string represents a line from the blob content.
     * Returns an empty list if the blob cannot be read.
     */
    public List<String> getBlobLines(ObjectID objectID) {
        Objects.requireNonNull(objectID, "objectID must not be null");
        try {
            ObjectStorage blob = loadObject(objectID, ObjectType.BLOB);
            String content = new String(blob.getContent(), StandardCharsets.UTF_8);
            return Arrays.asList(content.split("\\R", -1));

        } catch (IOException | NoSuchAlgorithmException e) {
            logger.error("Failed to read object {}", objectID.getValue(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Compresses the given content and writes it to the specified temporary file path.
     *
     * @param tempPath The temporary file path where the compressed content will be written.
     * @param content  The byte array representing the content to be compressed and written.
     * @throws IOException If an I/O error occurs during the compression or writing process.
     */
    private void writeCompressedContent(Path tempPath, byte[] content) throws IOException {
        byte[] compressed = compressData(content);
        Files.write(tempPath, compressed,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE);
    }

    /**
     * Atomically moves a file from a temporary location to its final destination
     * in the file system. This operation ensures that the move is performed atomically,
     * providing guarantees about file integrity and avoiding partial writes.
     *
     * @param tempPath  The path of the temporary file to be moved.
     * @param finalPath The target path where the file should be moved.
     * @throws IOException If an I/O error occurs during the move operation.
     */
    private void moveToFinalLocation(Path tempPath, Path finalPath) throws IOException {
        Files.move(tempPath, finalPath, StandardCopyOption.ATOMIC_MOVE);
    }

    /**
     * Attempts to delete a temporary file if it exists.
     * Failed deletion attempts are logged but do not interrupt the program flow.
     *
     * @param tempPath the path to the temporary file to be deleted
     * @throws IllegalArgumentException if tempPath is null
     */
    private void cleanupTempFile(Path tempPath) {
        if (tempPath == null) {
            throw new IllegalArgumentException("Temporary file path cannot be null");
        }

        try {
            boolean deleted = Files.deleteIfExists(tempPath);
            if (!deleted) {
                logger.debug("Temporary file {} did not exist during cleanup", tempPath);
            }
        } catch (SecurityException e) {
            logger.warn("Security restrictions prevented cleanup of temporary file: {}", tempPath, e);
        } catch (IOException e) {
            logger.warn("Failed to cleanup temporary file: {}", tempPath, e);
        }
    }


    /**
     * Generates a temporary name composed of a fixed prefix followed by a random sequence
     * of characters chosen from a predefined character set. The generated name is used
     * for temporary file creation or other similar use cases requiring unique identifiers.
     *
     * @return A randomly generated temporary name as a string.
     */
    private String generateTempName() {
        return TEMP_PREFIX + ThreadLocalRandom.current()
                .ints(TEMP_NAME_LENGTH, 0, TEMP_CHARS.length())
                .mapToObj(i -> String.valueOf(TEMP_CHARS.charAt(i)))
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append);
    }

    /**
     * Compresses the given byte array using the Deflater compression algorithm.
     * The method compresses the input data with the best speed setting and
     * returns the compressed data as a byte array.
     *
     * @param data The byte array representing the content to be compressed.
     *             It should not be null.
     * @return A byte array containing the compressed data.
     * @throws RuntimeException If an I/O error occurs during the compression process.
     */
    private byte[] compressData(byte[] data) {
        Deflater deflater = new Deflater(Deflater.BEST_SPEED);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length)) {
            deflater.setInput(data);
            deflater.finish();

            byte[] buffer = new byte[BUFFER_SIZE];
            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                outputStream.write(buffer, 0, count);
            }
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to compress data", e);
        } finally {
            deflater.end();
        }
    }

    /**
     * Decompresses the provided byte array, which is expected to be in a compressed format,
     * using the Inflater decompression algorithm. The method returns the original uncompressed
     * byte array.
     *
     * @param compressedData the byte array representing the compressed data to be decompressed.
     *                       Must not be null.
     * @return a byte array containing the decompressed data.
     * @throws RuntimeException if the decompression process fails due to data format issues or
     *                          an I/O error.
     */
    private byte[] uncompressData(byte[] compressedData) {
        Inflater inflater = new Inflater();
        try (ByteArrayOutputStream outputStream =
                     new ByteArrayOutputStream(compressedData.length)) {

            inflater.setInput(compressedData);

            byte[] buffer = new byte[BUFFER_SIZE];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                outputStream.write(buffer, 0, count);
            }

            return outputStream.toByteArray();

        } catch (DataFormatException | IOException e) {
            throw new RuntimeException("Failed to uncompress data", e);
        } finally {
            inflater.end();
        }
    }


}
