package duongtran.vctrl.actions;

import duongtran.vctrl.TestUtils;
import duongtran.vctrl.Workspace;
import duongtran.vctrl.diff.DiffResult;
import duongtran.vctrl.diff.EditScript;
import duongtran.vctrl.diff.EditType;
import duongtran.vctrl.diff.Hunk;
import duongtran.vctrl.index.Index;
import duongtran.vctrl.reportchanges.Status;
import duongtran.vctrl.reportchanges.StatusEntry;
import duongtran.vctrl.reportchanges.StatusType;
import duongtran.vctrl.storage.Database;
import duongtran.vctrl.storage.objects.Commit;
import duongtran.vctrl.utils.DirectoryNames;
import duongtran.vctrl.utils.Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

public class DiffActionTest {

    // Vctrl instances
    Workspace workspace;
    Database database;
    Path rootPath;
    Path vctrlPath;
    Path objectPath;

    // Mock directories
    Path firstDir;
    Path subFirstDir;
    Path secondDir;
    Path subSecondDir;

    // Mock files
    Path testFile11;
    Path testFile12;
    Path testFile111;
    Path testFile112;
    Path testFile21;
    Path testFile22;
    Path testFile211;
    Path testFile212;


    @BeforeEach
    void setup() throws IOException {
        TestUtils.createTestWorkspace();
        workspace = Workspace.getInstance();
        database = Database.getInstance();

        // Paths
        rootPath = workspace.getRootPath();
        vctrlPath = workspace.getVctrlPath();
        objectPath = vctrlPath.resolve(DirectoryNames.OBJECTS);

        // Initialize test directories and files
        firstDir = rootPath.resolve("firstDir");
        subFirstDir = firstDir.resolve("subFirstDir");
        secondDir = rootPath.resolve("secondDir");
        subSecondDir = secondDir.resolve("subSecondDir");

        testFile11 = firstDir.resolve("file11.txt");
        testFile12 = firstDir.resolve("file12.txt");
        testFile111 = subFirstDir.resolve("file111.txt");
        testFile112 = subFirstDir.resolve("file112.txt");
        testFile21 = secondDir.resolve("file21.txt");
        testFile22 = secondDir.resolve("file22.txt");
        testFile211 = subSecondDir.resolve("file211.txt");
        testFile212 = subSecondDir.resolve("file212.txt");

    }

    @AfterEach
    void tearDown() {
        TestUtils.removeWorkspace();
        workspace = null;
    }

    @Test
    void testExecuteDiffBetweenIndexAndWorkspace() {

        StatusAction statusAction = new StatusAction();
        AddAction addAction = new AddAction();
        CommitAction commitAction = new CommitAction();
        DiffAction diffAction = new DiffAction();

        try {

            // Create files and its contents in the firstDir
            Files.createDirectories(firstDir);
            TestUtils.writeText(testFile11,
                    "Test content 11 first line\n" +
                            "Test content 11 second line\n" +
                            "Test content 11 third line\n" +
                            "Test content 11 fourth line\n" +
                            "Test content 11 fifth line\n" +
                            "Test content 11 sixth line\n" +
                            "Test content 11 seventh line\n" +
                            "Test content 11 ninth line\n" +
                            "Test content 11 tenth line\n" +
                            "Test content 11 eleventh line\n" +
                            "Test content 11 twelve line\n" +
                            "Test content 11 thirteenth line"
            );
            TestUtils.writeText(testFile12, "Test content 12");

            // Create files and its contents in the subFirstDir
            Files.createDirectories(subFirstDir);
            TestUtils.writeText(testFile111, "Test content 111");
            TestUtils.writeText(testFile112, "Test content 112");

            // Add firstDir to staging and commit
            Index firstIndex = addAction.execute(firstDir);
            Commit firstCommit = commitAction.execute();

            // Execute status command first time
            Status firstActual = statusAction.execute();

            // Assert: After the first time, nothing changed
            Status firstExpected = new Status();
            assertEquals(firstExpected, firstActual);

            // Change contents of testFile11
            //      Update fourth line
            //      Delete fifth line
            //      Add eight line
            //      Add fourteenth line
            TestUtils.writeText(testFile11,
                    "Test content 11 first line\n" +
                            "Test content 11 second line\n" +
                            "Test content 11 third line\n" +
                            "Test content 11 fourth line updated\n" +
                            "Test content 11 sixth line\n" +
                            "Test content 11 seventh line\n" +
                            "Test content 11 eight line\n" +
                            "Test content 11 ninth line\n" +
                            "Test content 11 tenth line\n" +
                            "Test content 11 eleventh line\n" +
                            "Test content 11 twelve line\n" +
                            "Test content 11 thirteenth line\n" +
                            "Test content 11 fourteenth line"
            );

            // Execute status command a second time
            Status secondActual = statusAction.execute();

            // Assert: After modifying the two files, changes will be reported
            Status secondExpected = new Status();
            secondExpected.addEntry(new StatusEntry(testFile11, StatusType.WORKSPACE_MODIFIED));
            secondExpected.addWorkspaceModifiedMap(new StatusEntry(testFile11, StatusType.WORKSPACE_MODIFIED));
            assertEquals(secondExpected, secondActual);
            assertTrue(Utils.mapsEqual(secondExpected.getWorkspaceModifiedMap(), secondActual.getWorkspaceModifiedMap()));

            // Assert: Spot the differences between Index/Workspace
            DiffResult actualDiffResult = diffAction.execute(false);

            // Build expected differences result
            List<EditScript> editScripts1 = new ArrayList<>(List.of(
                    new EditScript(EditType.EQUAL, 2, 2, "Test content 11 second line")
                    , new EditScript(EditType.EQUAL, 3, 3, "Test content 11 third line")
                    , new EditScript(EditType.DELETE, 4, -1, "Test content 11 fourth line")
                    , new EditScript(EditType.DELETE, 5, -1, "Test content 11 fifth line")
                    , new EditScript(EditType.INSERT, -1, 4, "Test content 11 fourth line updated")
                    , new EditScript(EditType.EQUAL, 6, 5, "Test content 11 sixth line")
                    , new EditScript(EditType.EQUAL, 7, 6, "Test content 11 seventh line")
                    , new EditScript(EditType.INSERT, -1, 7, "Test content 11 eight line")
                    , new EditScript(EditType.EQUAL, 8, 8, "Test content 11 ninth line")
                    , new EditScript(EditType.EQUAL, 9, 9, "Test content 11 tenth line")
            ));
            Hunk hunk1 = new Hunk(
                    2 // oldStart
                    , 8 // oldLength
                    , 2 // newStart
                    , 8 // newLength
                    , editScripts1
            );

            List<EditScript> editScripts2 = new ArrayList<>(List.of(
                    new EditScript(EditType.EQUAL, 11, 11, "Test content 11 twelve line")
                    , new EditScript(EditType.EQUAL, 12, 12, "Test content 11 thirteenth line")
                    , new EditScript(EditType.INSERT, -1, 13, "Test content 11 fourteenth line")
            ));
            Hunk hunk2 = new Hunk(
                    11 // oldStart
                    , 2 // oldLength
                    , 11 // newStart
                    , 3 // newLength
                    , editScripts2
            );
            TreeMap<Path, List<Hunk>> hunkMap = new TreeMap<>();
            hunkMap.put(testFile11, new ArrayList<>(List.of(hunk1, hunk2)));
            DiffResult expectedDiffResult = new DiffResult(hunkMap);
            assertEquals(expectedDiffResult, actualDiffResult);

            // Add testFile11 and testFile112 to staging and commit
            Index secondIndex = addAction.execute(firstDir);
            Commit secondCommit = commitAction.execute();

            // Execute status command a third time
            Status thirdActual = statusAction.execute();

            // Assert: After the third time, nothing changed
            Status thirdExpected = new Status();
            assertEquals(thirdExpected, thirdActual);


        } catch (Exception ex) {
            ex.printStackTrace();
            fail();
        }
    }


    @Test
    void testExecuteDiffBetweenIndexAndHEAD() {

        StatusAction statusAction = new StatusAction();
        AddAction addAction = new AddAction();
        CommitAction commitAction = new CommitAction();
        DiffAction diffAction = new DiffAction();

        try {

            // Create files and its contents in the firstDir
            Files.createDirectories(firstDir);
            TestUtils.writeText(testFile11,
                    "Test content 11 first line\n" +
                            "Test content 11 second line\n" +
                            "Test content 11 third line\n" +
                            "Test content 11 fourth line\n" +
                            "Test content 11 fifth line\n" +
                            "Test content 11 sixth line\n" +
                            "Test content 11 seventh line\n" +
                            "Test content 11 ninth line\n" +
                            "Test content 11 tenth line\n" +
                            "Test content 11 eleventh line\n" +
                            "Test content 11 twelve line\n" +
                            "Test content 11 thirteenth line"
            );
            TestUtils.writeText(testFile12, "Test content 12");

            // Create files and its contents in the subFirstDir
            Files.createDirectories(subFirstDir);
            TestUtils.writeText(testFile111, "Test content 111");
            TestUtils.writeText(testFile112, "Test content 112");

            // Add firstDir to staging and commit
            Index firstIndex = addAction.execute(firstDir);
            Commit firstCommit = commitAction.execute();

            // Execute status command first time
            Status firstActual = statusAction.execute();

            // Assert: After the first time, nothing changed
            Status firstExpected = new Status();
            assertEquals(firstExpected, firstActual);

            // Change contents of testFile11
            //      Update fourth line
            //      Delete fifth line
            //      Add eight line
            //      Add fourteenth line
            TestUtils.writeText(testFile11,
                    "Test content 11 first line\n" +
                            "Test content 11 second line\n" +
                            "Test content 11 third line\n" +
                            "Test content 11 fourth line updated\n" +
                            "Test content 11 sixth line\n" +
                            "Test content 11 seventh line\n" +
                            "Test content 11 eight line\n" +
                            "Test content 11 ninth line\n" +
                            "Test content 11 tenth line\n" +
                            "Test content 11 eleventh line\n" +
                            "Test content 11 twelve line\n" +
                            "Test content 11 thirteenth line\n" +
                            "Test content 11 fourteenth line"
            );

            // Add the modified testFile11 to staging
            Index secondIndex = addAction.execute(testFile11);

            // Execute status command a second time
            Status secondActual = statusAction.execute();


            // Assert: After modifying the two files, changes will be reported
            Status secondExpected = new Status();
            secondExpected.addEntry(new StatusEntry(testFile11, StatusType.INDEX_MODIFIED));
            secondExpected.addIndexModifiedMap(new StatusEntry(testFile11, StatusType.INDEX_MODIFIED));
            assertEquals(secondExpected, secondActual);
            assertTrue(Utils.mapsEqual(secondExpected.getIndexModifiedMap(), secondActual.getIndexModifiedMap()));

            // Assert: Spot the differences between Index/Workspace
            DiffResult actualDiffResult = diffAction.execute(true);

            // Build expected differences result
            List<EditScript> editScripts1 = new ArrayList<>(List.of(
                    new EditScript(EditType.EQUAL, 2, 2, "Test content 11 second line")
                    , new EditScript(EditType.EQUAL, 3, 3, "Test content 11 third line")
                    , new EditScript(EditType.DELETE, 4, -1, "Test content 11 fourth line")
                    , new EditScript(EditType.DELETE, 5, -1, "Test content 11 fifth line")
                    , new EditScript(EditType.INSERT, -1, 4, "Test content 11 fourth line updated")
                    , new EditScript(EditType.EQUAL, 6, 5, "Test content 11 sixth line")
                    , new EditScript(EditType.EQUAL, 7, 6, "Test content 11 seventh line")
                    , new EditScript(EditType.INSERT, -1, 7, "Test content 11 eight line")
                    , new EditScript(EditType.EQUAL, 8, 8, "Test content 11 ninth line")
                    , new EditScript(EditType.EQUAL, 9, 9, "Test content 11 tenth line")
            ));
            Hunk hunk1 = new Hunk(
                    2 // oldStart
                    , 8 // oldLength
                    , 2 // newStart
                    , 8 // newLength
                    , editScripts1
            );

            List<EditScript> editScripts2 = new ArrayList<>(List.of(
                    new EditScript(EditType.EQUAL, 11, 11, "Test content 11 twelve line")
                    , new EditScript(EditType.EQUAL, 12, 12, "Test content 11 thirteenth line")
                    , new EditScript(EditType.INSERT, -1, 13, "Test content 11 fourteenth line")
            ));
            Hunk hunk2 = new Hunk(
                    11 // oldStart
                    , 2 // oldLength
                    , 11 // newStart
                    , 3 // newLength
                    , editScripts2
            );
            TreeMap<Path, List<Hunk>> hunkMap = new TreeMap<>();
            hunkMap.put(testFile11, new ArrayList<>(List.of(hunk1, hunk2)));
            DiffResult expectedDiffResult = new DiffResult(hunkMap);
            assertEquals(expectedDiffResult, actualDiffResult);

            // Commit the second time
            Commit secondCommit = commitAction.execute();

            // Execute status command a third time
            Status thirdActual = statusAction.execute();

            // Assert: After the third time, nothing changed
            Status thirdExpected = new Status();
            assertEquals(thirdExpected, thirdActual);


        } catch (Exception ex) {
            ex.printStackTrace();
            fail();
        }
    }

}
