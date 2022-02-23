package com.hedera.services.legacy.core.jproto;

/*-
 * ‌
 * Hedera Services Node
 * ​
 * Copyright (C) 2018 - 2022 Hedera Hashgraph, LLC
 * ​
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ‍
 */

import org.junit.jupiter.api.Test;

import static com.swirlds.common.CommonUtils.unhex;
import static org.junit.jupiter.api.Assertions.*;

class JContractAliasKeyTest {
	@Test
	void onlyValidIfAddressIsLength20() {
		final byte[] evmAddress = unhex("aaaaaaaaaaaaaaaaaaaaaaaa9abcdefabcdefbbb");
		final var empty = new JContractAliasKey(0, 0, new byte[0]);
		final var invalid = new JContractAliasKey(0, 0, "NOPE".getBytes());
		final var valid = new JContractAliasKey(0, 0, evmAddress);

		assertFalse(empty.isValid());
		assertFalse(invalid.isValid());
		assertTrue(valid.isValid());
	}
}