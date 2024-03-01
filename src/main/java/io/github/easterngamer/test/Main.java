package io.github.easterngamer.test;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class Main {
    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        Firestore firestore = FirestoreOptions.newBuilder()
                .setCredentials(GoogleCredentials.fromStream(Objects.requireNonNull(Main.class.getClassLoader().getResourceAsStream("firebase.json"))))
                .build().getService();
        Map<String, Object> expectedObject = new HashMap<>();
        expectedObject.put("string value", "some string value");
        expectedObject.put("int64 value", 1209532542454341714L);
        expectedObject.put("double value", 1209532542454341714D);
        expectedObject.put("map value", Map.of(
                "string value", "some map string",
                "int64 value", 1209532542454341714L
        ));
        expectedObject.put("array map value", List.of(
                Map.of(
                        "string value", "some array map string 1",
                        "int64 value", 1209532542454341714L
                ),
                Map.of(
                        "string value", "some array map string 2",
                        "int64 value", 1209532542454341714L
                )
        ));
        try {
            firestore.document("test/item")
                    .create(expectedObject)
                    .get();
        } catch (ExecutionException e) {
            firestore.document("test/item")
                    .update(expectedObject)
                    .get();
        }

        Map<String, Object> documentDataFromFirebase = firestore.document("test/item")
                .get()
                .get()
                .getData();
        compare("From Direct Get", expectedObject, documentDataFromFirebase, 0);

        firestore.document("test/item")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        Map<String, Object> documentDataFromConsoleUpdate = value.getData();
                        compare("From Console Sync", expectedObject, documentDataFromConsoleUpdate, 0);
                    }
                });
        Thread.sleep(Long.MAX_VALUE);
    }

    public static boolean compare(String tag, Map<String, Object> expectedData, Map<String, Object> other, int depth) {
        boolean[] check = new boolean[] {true};

        final String indent = " ".repeat(depth*2);
        System.out.println(indent + "Comparing " + tag + "...");
        expectedData.forEach((key, data) -> {
            Object otherData = other.get(key);
            if (data instanceof List<?> expectedDataList && otherData instanceof List<?> otherDataList) {
                for (int i = 0; i < expectedDataList.size(); i++) {
                    Object currentElementOfListExpected = expectedDataList.get(i);
                    Object currentElementOfOtherList = otherDataList.get(i);
                    if (currentElementOfListExpected instanceof Map<?,?> expectedDataMap && currentElementOfOtherList instanceof Map<?,?> otherDataMap) {
                        if (!compare("Sub Map in Array", (Map<String, Object>) expectedDataMap, (Map<String, Object>) otherDataMap, depth+1)) {
                            check[0] = false;
                        }
                    } else if (!currentElementOfListExpected.equals(currentElementOfListExpected)) {
                        check[0] = false;
                        System.out.println("Data Mismatch for " + key + "! Expected \"" + data + "\" of type " + data.getClass().getSimpleName() + " but got \"" + otherData + "\" of type " + otherData.getClass().getSimpleName());
                    }
                }
            } else if (data instanceof Map<?,?> expectedDataMap && otherData instanceof Map<?,?> otherDataMap) {
                if (!compare("Sub Map in Document", (Map<String, Object>) expectedDataMap, (Map<String, Object>) otherDataMap, depth+1)) {
                    check[0] = false;
                }
            } else if (!data.equals(otherData)) {
                check[0] = false;
                System.out.println(indent + "Data Mismatch for " + key + "! Expected " + data + " of type " + data.getClass().getSimpleName() + " but got " + otherData + " of type " + otherData.getClass().getSimpleName());
            }
        });
        if (check[0]) {
            System.out.println(indent + tag + ": No data mismatch with expected and retrieved.");
        }
        return check[0];
    }
}
