package com.hedera.services.state.merkle.virtual.persistence.fcmmap;

import com.hedera.services.state.merkle.virtual.persistence.FCSlotIndex;
import com.hedera.services.state.merkle.virtual.persistence.fcmmap.FCVirtualMapTestUtils.LongVKey;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BinFileTest {
    public static final Path STORE_PATH = Path.of("store");

    static {
        System.out.println("STORE_PATH = " + STORE_PATH.toAbsolutePath());
    }

    private static final Random RANDOM = new Random(1234);

    private static Path getTempFile() throws IOException {
        File tempFile = File.createTempFile("BinFileTest",".dat");
        System.out.println("tempFile = " + tempFile.getAbsolutePath());
        tempFile.deleteOnExit();
        return tempFile.toPath();
    }

    /**
     * test basic data storage and retrieval for a single version
     */
    @Test
    public void createSomeDataAndReadBack() throws IOException {
        final int COUNT = 10_000;
//        final int COUNT = 1;
        // create and open file
        Path file = getTempFile();
        BinFile<LongVKey> binFile = new BinFile<>(1,file,8,150,100,20,20);
        System.out.printf("BinFile size: %,.1f Mb\n",(double) Files.size(file)/(1024d*1024d));
        // create some data for a number of accounts
        for (int i = 0; i < COUNT; i++) {
            LongVKey key = new LongVKey(i);
            binFile.putSlot(0,key.hashCode(), key,i);
        }
        // read back and check that data
        for (int i = 0; i < COUNT; i++) {
            LongVKey key = new LongVKey(i);
            long result = binFile.getSlot(0,key.hashCode(), key);
            assertEquals(i, result);
        }

        // read back random and check that data
        for (int j = 0; j < COUNT; j++) {
            int i = RANDOM.nextInt(COUNT);
            LongVKey key = new LongVKey(i);
            long result = binFile.getSlot(0,key.hashCode(), key);
            assertEquals(i, result);
        }
        // close
        binFile.close();
    }

    @Test
    public void testRelease() throws IOException {
        // Create and open the file. I'm only going to allow a maximum of 10 mutations per queue, and throughout the
        // test I will add and remove things and keep track of how many mutations there should be. Eventually I will
        // overflow it to trigger an exception to prove the number of mutations is exactly as expected.
        Path file = getTempFile();
        BinFile<LongVKey> binFile = new BinFile<>(0, file,8,5,10,10,1);

        // Release version 0 (nothing has been written yet, should have no effect)
        binFile.versionChanged(0, 1);
        binFile.releaseVersion(0);

        // Write a single item for version 1. Then release it. The mutation queue should still exist, but
        // with a single "RELEASED" item.
        final var key = new LongVKey(1001001);
        binFile.putSlot(1, key.hashCode(), key, 111);
        binFile.versionChanged(1, 2);
        binFile.releaseVersion(1);

        // Now write another value for that key. The first key should have been deleted as a consequence.
        // At this point in time, there should be a single mutation in the queue.
        assertEquals(111, binFile.getSlot(2, key.hashCode(), key));
        binFile.putSlot(2, key.hashCode(), key, 222);
        assertEquals(222, binFile.getSlot(2, key.hashCode(), key));

        // At this point, we have a single mutation. If I write a few more times to the same version, it shouldn't
        // result in any additional mutations
        for (int i=0; i<20; i++) {
            assertEquals(223 + i - 1, binFile.getSlot(2, key.hashCode(), key));
            binFile.putSlot(2, key.hashCode(), key, 223 + i);
            assertEquals(223 + i, binFile.getSlot(2, key.hashCode(), key));
        }

        // Now add versions to fill up the mutation queue
        binFile.versionChanged(2, 3);
        assertEquals(242, binFile.getSlot(3, key.hashCode(), key));
        binFile.putSlot(3, key.hashCode(), key, 333); // TODO we never check that "versionChanged" happens before a new version shows up!!
        assertEquals(333, binFile.getSlot(3, key.hashCode(), key));
        System.out.println(binFile.printMutationQueue(924));

        binFile.versionChanged(3, 4);
        assertEquals(333, binFile.getSlot(4, key.hashCode(), key));
        binFile.putSlot(4, key.hashCode(), key, 444);
        assertEquals(333, binFile.getSlot(3, key.hashCode(), key));
        assertEquals(444, binFile.getSlot(4, key.hashCode(), key));
        System.out.println(binFile.printMutationQueue(924));

        binFile.versionChanged(4, 5);
        assertEquals(444, binFile.getSlot(5, key.hashCode(), key));
        binFile.putSlot(5, key.hashCode(), key, 555);
        assertEquals(333, binFile.getSlot(3, key.hashCode(), key));
        assertEquals(444, binFile.getSlot(4, key.hashCode(), key));
        assertEquals(555, binFile.getSlot(5, key.hashCode(), key));
        System.out.println(binFile.printMutationQueue(924));

        binFile.versionChanged(5, 6);
        assertEquals(555, binFile.getSlot(6, key.hashCode(), key));
        binFile.putSlot(6, key.hashCode(), key, 666);
        assertEquals(333, binFile.getSlot(3, key.hashCode(), key));
        assertEquals(444, binFile.getSlot(4, key.hashCode(), key));
        assertEquals(555, binFile.getSlot(5, key.hashCode(), key));
        assertEquals(666, binFile.getSlot(6, key.hashCode(), key));
        System.out.println(binFile.printMutationQueue(924));

        binFile.versionChanged(6, 7);
        assertEquals(666, binFile.getSlot(7, key.hashCode(), key));
        binFile.putSlot(7, key.hashCode(), key, 777);
        assertEquals(333, binFile.getSlot(3, key.hashCode(), key));
        assertEquals(444, binFile.getSlot(4, key.hashCode(), key));
        assertEquals(555, binFile.getSlot(5, key.hashCode(), key));
        assertEquals(666, binFile.getSlot(6, key.hashCode(), key));
        assertEquals(777, binFile.getSlot(7, key.hashCode(), key));
        System.out.println(binFile.printMutationQueue(924));

        binFile.versionChanged(7, 8);
        assertEquals(777, binFile.getSlot(8, key.hashCode(), key));
        binFile.putSlot(8, key.hashCode(), key, 888);
        assertEquals(333, binFile.getSlot(3, key.hashCode(), key));
        assertEquals(444, binFile.getSlot(4, key.hashCode(), key));
        assertEquals(555, binFile.getSlot(5, key.hashCode(), key));
        assertEquals(666, binFile.getSlot(6, key.hashCode(), key));
        assertEquals(777, binFile.getSlot(7, key.hashCode(), key));
        assertEquals(888, binFile.getSlot(8, key.hashCode(), key));
        System.out.println(binFile.printMutationQueue(924));

        binFile.versionChanged(8, 9);
        assertEquals(888, binFile.getSlot(9, key.hashCode(), key));
        binFile.putSlot(9, key.hashCode(), key, 999);
        assertEquals(333, binFile.getSlot(3, key.hashCode(), key));
        assertEquals(444, binFile.getSlot(4, key.hashCode(), key));
        assertEquals(555, binFile.getSlot(5, key.hashCode(), key));
        assertEquals(666, binFile.getSlot(6, key.hashCode(), key));
        assertEquals(777, binFile.getSlot(7, key.hashCode(), key));
        assertEquals(888, binFile.getSlot(8, key.hashCode(), key));
        assertEquals(999, binFile.getSlot(9, key.hashCode(), key));
        System.out.println(binFile.printMutationQueue(924));

        binFile.versionChanged(9, 10);
        assertEquals(999, binFile.getSlot(10, key.hashCode(), key));
        binFile.putSlot(10, key.hashCode(), key, 1010);
        assertEquals(333, binFile.getSlot(3, key.hashCode(), key));
        assertEquals(444, binFile.getSlot(4, key.hashCode(), key));
        assertEquals(555, binFile.getSlot(5, key.hashCode(), key));
        assertEquals(666, binFile.getSlot(6, key.hashCode(), key));
        assertEquals(777, binFile.getSlot(7, key.hashCode(), key));
        assertEquals(888, binFile.getSlot(8, key.hashCode(), key));
        assertEquals(999, binFile.getSlot(9, key.hashCode(), key));
        assertEquals(1010, binFile.getSlot(10, key.hashCode(), key));
        System.out.println(binFile.printMutationQueue(924));

        binFile.versionChanged(10, 11);
        assertEquals(1010, binFile.getSlot(11, key.hashCode(), key));
        binFile.putSlot(11, key.hashCode(), key, 1111);
        assertEquals(333, binFile.getSlot(3, key.hashCode(), key));
        assertEquals(444, binFile.getSlot(4, key.hashCode(), key));
        assertEquals(555, binFile.getSlot(5, key.hashCode(), key));
        assertEquals(666, binFile.getSlot(6, key.hashCode(), key));
        assertEquals(777, binFile.getSlot(7, key.hashCode(), key));
        assertEquals(888, binFile.getSlot(8, key.hashCode(), key));
        assertEquals(999, binFile.getSlot(9, key.hashCode(), key));
        assertEquals(1010, binFile.getSlot(10, key.hashCode(), key));
        assertEquals(1111, binFile.getSlot(11, key.hashCode(), key));
        System.out.println(binFile.printMutationQueue(924));

        // Now lets pick a couple versions to release in the middle and at the end
        binFile.releaseVersion(6);
        System.out.println(binFile.printMutationQueue(924));
        binFile.releaseVersion(8);
        System.out.println(binFile.printMutationQueue(924));
        binFile.releaseVersion(9);
        System.out.println(binFile.printMutationQueue(924));

        // Now lets add new versions to fill up the queue again (another 4 to go!)
        binFile.versionChanged(11, 12);
        binFile.releaseVersion(11);
        System.out.println(binFile.printMutationQueue(924));
        assertEquals(242, binFile.getSlot(2, key.hashCode(), key));
        assertEquals(333, binFile.getSlot(3, key.hashCode(), key));
        assertEquals(444, binFile.getSlot(4, key.hashCode(), key));
        assertEquals(555, binFile.getSlot(5, key.hashCode(), key));
        assertEquals(777, binFile.getSlot(7, key.hashCode(), key));
        assertEquals(1010, binFile.getSlot(10, key.hashCode(), key));
        assertEquals(1111, binFile.getSlot(12, key.hashCode(), key));
        binFile.putSlot(12, key.hashCode(), key, 1212);
        assertEquals(1212, binFile.getSlot(12, key.hashCode(), key));
        System.out.println(binFile.printMutationQueue(924));

        binFile.versionChanged(12, 13);
        assertEquals(242, binFile.getSlot(2, key.hashCode(), key));
        assertEquals(333, binFile.getSlot(3, key.hashCode(), key));
        assertEquals(444, binFile.getSlot(4, key.hashCode(), key));
        assertEquals(555, binFile.getSlot(5, key.hashCode(), key));
        assertEquals(777, binFile.getSlot(7, key.hashCode(), key));
        assertEquals(1010, binFile.getSlot(10, key.hashCode(), key));
        assertEquals(1212, binFile.getSlot(12, key.hashCode(), key));
        assertEquals(1212, binFile.getSlot(13, key.hashCode(), key));
        binFile.putSlot(13, key.hashCode(), key, 1313);
        assertEquals(1313, binFile.getSlot(13, key.hashCode(), key));

        binFile.versionChanged(13, 14);
        assertEquals(242, binFile.getSlot(2, key.hashCode(), key));
        assertEquals(333, binFile.getSlot(3, key.hashCode(), key));
        assertEquals(444, binFile.getSlot(4, key.hashCode(), key));
        assertEquals(555, binFile.getSlot(5, key.hashCode(), key));
        assertEquals(777, binFile.getSlot(7, key.hashCode(), key));
        assertEquals(1010, binFile.getSlot(10, key.hashCode(), key));
        assertEquals(1212, binFile.getSlot(12, key.hashCode(), key));
        assertEquals(1313, binFile.getSlot(13, key.hashCode(), key));
        assertEquals(1313, binFile.getSlot(14, key.hashCode(), key));
        binFile.putSlot(14, key.hashCode(), key, 1414);
        assertEquals(1414, binFile.getSlot(14, key.hashCode(), key));

        binFile.versionChanged(14, 15);
        assertEquals(242, binFile.getSlot(2, key.hashCode(), key));
        assertEquals(333, binFile.getSlot(3, key.hashCode(), key));
        assertEquals(444, binFile.getSlot(4, key.hashCode(), key));
        assertEquals(555, binFile.getSlot(5, key.hashCode(), key));
        assertEquals(777, binFile.getSlot(7, key.hashCode(), key));
        assertEquals(1010, binFile.getSlot(10, key.hashCode(), key));
        assertEquals(1212, binFile.getSlot(12, key.hashCode(), key));
        assertEquals(1313, binFile.getSlot(13, key.hashCode(), key));
        assertEquals(1414, binFile.getSlot(14, key.hashCode(), key));
        assertEquals(1414, binFile.getSlot(15, key.hashCode(), key));
        binFile.putSlot(15, key.hashCode(), key, 1515);
        assertEquals(1515, binFile.getSlot(15, key.hashCode(), key));

        // TODO put a lot of asserts in to read back values for every version, and what happens if we lookup a released version?

        // Now it is time to tip the scales!
        assertThrows(IllegalStateException.class, () -> binFile.putSlot(99, key.hashCode(), key, 9999));

        // close
        binFile.close();
    }

    @Test
    public void testTwoReleasesInARow() throws IOException {
        final var key = new LongVKey(1001001);
        final var file = getTempFile();
        final var binFile = new BinFile<>(1, file,8,5,10,20,1);

        binFile.putSlot(1, key.hashCode(), key, 1);
        binFile.versionChanged(1, 2);
        binFile.putSlot(2, key.hashCode(), key, 2);
        binFile.versionChanged(2, 3);
        binFile.putSlot(3, key.hashCode(), key, 3);
        binFile.releaseVersion(1);
        binFile.releaseVersion(2);
        binFile.versionChanged(3, 4);
        binFile.putSlot(4, key.hashCode(), key, 4);
        assertEquals(3, binFile.getSlot(3, key.hashCode(), key));
        assertEquals(4, binFile.getSlot(4, key.hashCode(), key));
    }

    @Test
    public void testTwoReleasesInARowAndSkippingAMutation() throws IOException {
        final var key = new LongVKey(1001001);
        final var file = getTempFile();
        final var binFile = new BinFile<>(1, file,8,5,10,20,1);

        binFile.putSlot(1, key.hashCode(), key, 1);
        binFile.versionChanged(1, 2);
        binFile.putSlot(2, key.hashCode(), key, 2);
        binFile.versionChanged(2, 3);
        binFile.releaseVersion(1);
        binFile.releaseVersion(2);
        binFile.versionChanged(3, 4);
        binFile.putSlot(4, key.hashCode(), key, 4);
        assertEquals(2, binFile.getSlot(3, key.hashCode(), key));
        assertEquals(4, binFile.getSlot(4, key.hashCode(), key));
    }

    @Test
    public void testMoreReleaseScenarios() throws IOException {
        final var key = new LongVKey(1001001);
        final var file = getTempFile();
        final var binFile = new BinFile<>(0, file,8,5,10,20,1);

        // Create 12 versions and 12 mutations
        for (int i=0; i<12; i++) {
            binFile.putSlot(i, key.hashCode(), key, i);
            binFile.versionChanged(i, i+1);
        }

        // Selectively nuke a few
        binFile.releaseVersion(3);
        binFile.releaseVersion(1);
        binFile.releaseVersion(8);

        // Verify that everything is read out correctly
        for (int i=0; i<12; i++) {
            if (i == 3 || i == 1 || i == 8) continue;
            assertEquals(i, binFile.getSlot(i, key.hashCode(), key));
        }

        // Add one new modification. This would cause a sweep.
        binFile.putSlot(12, key.hashCode(), key, 12);

        // Verify that everything is read out correctly
        for (int i=0; i<12; i++) {
            if (i == 3 || i == 1 || i == 8) continue;
            assertEquals(i, binFile.getSlot(i, key.hashCode(), key));
        }
    }

    @Test
    public void testDelete() throws IOException {
        // create and open file
        Path file = getTempFile();
        BinFile<LongVKey> binFile = new BinFile<>(1,file,8,5,5,20,1);
        // create some data for a number of accounts
        for (int i = 0; i < 10; i++) {
            LongVKey key = new LongVKey(i);
            binFile.putSlot(0,key.hashCode(), key,i);
        }
        // read back and check that data
        for (int i = 0; i < 10; i++) {
            LongVKey key = new LongVKey(i);
            long result = binFile.getSlot(0,key.hashCode(), key);
            assertEquals(i, result);
        }
        // delete 3 and 7
        LongVKey key3 = new LongVKey(3);
        binFile.removeKey(0,key3.hashCode(), key3);
        LongVKey key7 = new LongVKey(7);
        binFile.removeKey(0,key7.hashCode(), key7);
        // check data
        for (int i = 0; i < 10; i++) {
            LongVKey key = new LongVKey(i);
            long result = binFile.getSlot(0, key.hashCode(), key);
            assertEquals((i != 3 && i != 7) ? i : FCSlotIndex.NOT_FOUND_LOCATION, result);
        }
        // close
        binFile.close();
    }

    @Test
    public void versionsTest() throws IOException {
        // create and open file
        Path file = getTempFile();
        BinFile<LongVKey> binFile = new BinFile<>(1,file,8,20,20,20,5);
        System.out.printf("BinFile size: %,.1f Mb\n",(double) Files.size(file)/(1024d*1024d));
        // create key array
        LongVKey[] keys = new LongVKey[20];
        int[] keyHashs = new int[20];
        for (int i = 0; i < 20; i++) {
            keys[i] = new LongVKey(i);
            keyHashs[i] = keys[i].hashCode();
        }
        // create 20 values in version 100
        for (int i = 0; i < 20; i++) {
            binFile.putSlot(100,keyHashs[i], keys[i],100+i);
        }
        System.out.println(binFile.printMutationQueues(new LongVKey()));
        // read back and check that data
        for (int i = 0; i < 20; i++) {
            assertEquals(100+i, binFile.getSlot(100,keyHashs[i], keys[i]));
        }
        // create 10 values in version 200
        binFile.versionChanged(100,200);
        for (int i = 10; i < 20; i++) {
            binFile.putSlot(200,keyHashs[i], keys[i],200+i);
        }
        System.out.println(binFile.printMutationQueues(new LongVKey()));
        // check the data is still correct for version 100
        for (int i = 0; i < 20; i++) {
            assertEquals(100+i, binFile.getSlot(100,keyHashs[i], keys[i]));
        }
        // check the data is correct for version 200
        for (int i = 0; i < 20; i++) {
            long result = binFile.getSlot(200,keyHashs[i], keys[i]);
            assertEquals((i<10) ? 100+i : 200+i, result);
        }
        // delete one from version 200
        binFile.removeKey(200,keyHashs[5], keys[5]);
        System.out.println(binFile.printMutationQueues(new LongVKey()));
        // check it is deleted in 200
        assertEquals(FCSlotIndex.NOT_FOUND_LOCATION, binFile.getSlot(200,keyHashs[5], keys[5]));
        // check it is not deleted in 100
        assertEquals(105, binFile.getSlot(100,keyHashs[5], keys[5]));
        // add version 300 for key 5
        {
            binFile.versionChanged(200,300);
            binFile.putSlot(300, keyHashs[5], keys[5], 305);
            System.out.println(binFile.printMutationQueues(new LongVKey()));
            // check all values again
            assertEquals(305, binFile.getSlot(300,keyHashs[5], keys[5]));
            assertEquals(FCSlotIndex.NOT_FOUND_LOCATION, binFile.getSlot(200,keyHashs[5], keys[5]));
            assertEquals(105, binFile.getSlot(100,keyHashs[5], keys[5]));
        }
        // now release versions 100 and 200 and check all data is the same
        {
            binFile.releaseVersion(100);
            binFile.releaseVersion(200);
            binFile.versionChanged(300,400);
            System.out.println(binFile.printMutationQueues(new LongVKey()));
            // add new data for all keys for version 400 , this should cause all mutations for 100 and 200 to be cleaned up
            for (int i = 0; i < 20; i++) {
                binFile.putSlot(400,keyHashs[i], keys[i],400+i);
            }
            System.out.println(binFile.printMutationQueues(new LongVKey()));
            // check all data in 400 is correct
            for (int i = 0; i < 20; i++) {
                assertEquals(400+i, binFile.getSlot(400,keyHashs[i], keys[i]));
            }
            // check all data in 300 is correct
            System.out.println(binFile.printMutationQueues(new LongVKey()));
            for (int i = 0; i < 20; i++) {
                long result = binFile.getSlot(300,keyHashs[i], keys[i]);
                if (i == 5) {
                    assertEquals(305, binFile.getSlot(300,keyHashs[5], keys[5]));
                } else {
                    System.out.println("i = " + i);
                    assertEquals((i < 10) ? 100 + i : 200 + i, result);
                }
            }
        }
        // close
        binFile.close();
    }

}
