package duongtran.example.storage;

import duongtran.example.storage.objects.Blob;
import duongtran.example.storage.objects.Tree;
import duongtran.example.utils.DirectoryNames;
import duongtran.example.utils.HexUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.Deflater;

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

    private Database() {}

    public static void initialize() {
        File gitPath = new File(DirectoryNames.WORKING_DIRECTORY, DirectoryNames.ROOT_DIR_NAME);
        File dbPath = new File(gitPath, DirectoryNames.OBJECTS);
        getInstance().dbPath = dbPath.toPath();
    }

    public static Database getInstance() {
        if (instance == null) {
            instance = new Database();
        }
        return instance;
    }


    /**
     * Initialize a database with the path in the disk.
     * @param dbPath - the directory path (.vctrl/objects)
     * @throws IllegalArgumentException if dbPath is null
     */
    public Database(Path dbPath) {
        if (dbPath == null) {
            throw new IllegalArgumentException("Database path cannot be null");
        }
        this.dbPath = dbPath;
    }


    /**
     * Stores a blob object in the database.
     *
     * @param object The blob objects to store
     * @throws IOException If failed to write the object
     * @throws IllegalArgumentException If an object is null
     */
    public String store(ObjectStorage object) throws IOException, NoSuchAlgorithmException {
        if (object == null) {
            throw new IllegalArgumentException("Blob object cannot be null");
        }
        byte[] content = object.formatContent();
        object.calculateOid(content);
        writeObject(object.getOid(), content);
        return object.getOid();
    }


    /**
     * Writes a binary object with a specific object ID to the database. The method first
     * writes the compressed content to a temporary file, then atomically moves it to its final
     * location. If any error occurs during the process, the temporary file is cleaned up.
     *
     * @param oid The object ID, typically a hashed value used as the filename.
     * @param content The byte array representing the content of the object to be stored.
     * @throws IOException If an I/O error occurs during directory creation, writing the content,
     *                     or moving the file to its final location.
     */
    private void writeObject(String oid, byte[] content) throws IOException {
        Path objectPath = constructObjectPath(oid);
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
     *         where the object resides or should reside.
     */
    private Path constructObjectPath(String oid) {
        return dbPath.resolve(oid.substring(0, 2))
                .resolve(oid.substring(2));
    }

    /**
     * Compresses the given content and writes it to the specified temporary file path.
     *
     * @param tempPath The temporary file path where the compressed content will be written.
     * @param content The byte array representing the content to be compressed and written.
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
     * @param tempPath The path of the temporary file to be moved.
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


}
