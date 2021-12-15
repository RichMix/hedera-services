package com.hedera.services.records;

/*-
 * ‌
 * Hedera Services Node
 * ​
 * Copyright (C) 2018 - 2021 Hedera Hashgraph, LLC
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

import com.hedera.services.state.submerkle.ExpirableTxnRecord;
import com.hederahashgraph.api.proto.java.TransactionBody;

public class InProgressChildRecord {
	private final int sourceId;
	private final TransactionBody.Builder syntheticBody;
	private final ExpirableTxnRecord.Builder recordBuilder;

	public InProgressChildRecord(
			final int sourceId,
			final TransactionBody.Builder syntheticBody,
			final ExpirableTxnRecord.Builder recordBuilder
	) {
		this.sourceId = sourceId;
		this.syntheticBody = syntheticBody;
		this.recordBuilder = recordBuilder;
	}

	public int getSourceId() {
		return sourceId;
	}

	public TransactionBody.Builder getSyntheticBody() {
		return syntheticBody;
	}

	public ExpirableTxnRecord.Builder getRecordBuilder() {
		return recordBuilder;
	}
}