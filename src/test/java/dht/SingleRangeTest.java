/*
 * Copyright 2021 (c) Odnoklassniki
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dht;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for single node range API.
 *
 * @author Vadim Tsesko
 */
class SingleRangeTest extends TestBase {

    private static byte[] chunkOf(
            String key,
            String value) {
        return (key + '\n' + value).getBytes();
    }

    @ServiceTest(stage = 6)
    void emptyKey(ServiceInfo service) throws Exception {
        assertEquals(400, service.range("", "").statusCode());
        assertEquals(400, service.upsert("", new byte[]{0}).statusCode());
    }

    @ServiceTest(stage = 6)
    void absentParameterRequest(ServiceInfo service) throws Exception {
        assertEquals(
                400,
                client.send(service.request("/v0/entities").GET().build(),
                        HttpResponse.BodyHandlers.ofByteArray()).statusCode()
        );
        assertEquals(
                400,
                client.send(service.request("/v0/entities?end=end").GET().build(),
                        HttpResponse.BodyHandlers.ofByteArray()).statusCode()
        );
    }

    @ServiceTest(stage = 6)
    void getAbsent(ServiceInfo service) throws Exception {
        HttpResponse<byte[]> response = service.range("absent0", "absent1");
        assertEquals(200, response.statusCode());
        assertEquals(0, response.body().length);
    }

    @ServiceTest(stage = 6)
    void single(ServiceInfo service) throws Exception {
        String prefix = "single";
        String key = prefix + 1;
        String value = "value1";

        // Insert
        assertEquals(201, service.upsert(key, value.getBytes()).statusCode());

        // Check
        {
            HttpResponse<byte[]> response = service.range(key, prefix + 2);
            assertEquals(200, response.statusCode());
            assertArrayEquals(chunkOf(key, value), response.body());
        }

        // Excluding the key
        {
            HttpResponse<byte[]> response = service.range("a", key);
            assertEquals(200, response.statusCode());
            assertEquals(0, response.body().length);
        }

        // After the key
        {
            HttpResponse<byte[]> response = service.range(prefix + 2, prefix + 3);
            assertEquals(200, response.statusCode());
            assertEquals(0, response.body().length);
        }
    }

    @ServiceTest(stage = 6)
    void triple(ServiceInfo service) throws Exception {
        String prefix = "triple";
        String value1 = "value1";
        String value2 = "";
        String value3 = "value3";

        // Insert reversed
        assertEquals(201, service.upsert(prefix + 3, value3.getBytes()).statusCode());
        assertEquals(201, service.upsert(prefix + 2, value2.getBytes()).statusCode());
        assertEquals(201, service.upsert(prefix + 1, value1.getBytes()).statusCode());

        // Check all
        {
            byte[] chunk1 = chunkOf(prefix + 1, value1);
            byte[] chunk2 = chunkOf(prefix + 2, value2);
            byte[] chunk3 = chunkOf(prefix + 3, value3);
            byte[] expected = new byte[chunk1.length + chunk2.length + chunk3.length];
            System.arraycopy(chunk1, 0, expected, 0, chunk1.length);
            System.arraycopy(chunk2, 0, expected, chunk1.length, chunk2.length);
            System.arraycopy(chunk3, 0, expected, expected.length - chunk3.length, chunk3.length);

            HttpResponse<byte[]> response = service.range(prefix + 1, prefix + 4);
            assertEquals(200, response.statusCode());
            assertArrayEquals(expected, response.body());
        }

        // To the left
        {
            HttpResponse<byte[]> response = service.range(prefix + 0, prefix + 1);
            assertEquals(200, response.statusCode());
            assertEquals(0, response.body().length);
        }

        // First left
        {
            HttpResponse<byte[]> response = service.range(prefix + 0, prefix + 2);
            assertEquals(200, response.statusCode());
            assertArrayEquals(chunkOf(prefix + 1, value1), response.body());
        }

        // First point
        {
            HttpResponse<byte[]> response = service.range(prefix + 1, prefix + 2);
            assertEquals(200, response.statusCode());
            assertArrayEquals(chunkOf(prefix + 1, value1), response.body());
        }

        // Second point
        {
            HttpResponse<byte[]> response = service.range(prefix + 2, prefix + 3);
            assertEquals(200, response.statusCode());
            assertArrayEquals(chunkOf(prefix + 2, value2), response.body());
        }

        // Third point
        {
            HttpResponse<byte[]> response = service.range(prefix + 3, prefix + 4);
            assertEquals(200, response.statusCode());
            assertArrayEquals(chunkOf(prefix + 3, value3), response.body());
        }

        // To the right
        {
            HttpResponse<byte[]> response = service.range(prefix + 4, null);
            assertEquals(200, response.statusCode());
            assertEquals(0, response.body().length);
        }
    }
}
