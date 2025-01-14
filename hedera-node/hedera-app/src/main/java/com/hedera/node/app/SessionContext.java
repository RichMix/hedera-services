/*
 * Copyright (C) 2020-2022 Hedera Hashgraph, LLC
 *
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
 */
package com.hedera.node.app;

import static java.util.Objects.requireNonNull;

import com.google.protobuf.Parser;
import com.hederahashgraph.api.proto.java.Query;
import com.hederahashgraph.api.proto.java.SignedTransaction;
import com.hederahashgraph.api.proto.java.Transaction;
import com.hederahashgraph.api.proto.java.TransactionBody;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * This record keeps a list of everything that is used per-thread
 *
 * @param queryParser a parser for {@link Query}
 * @param txParser a parser for {@link Transaction}
 * @param signedParser a parser for {@link SignedTransaction}
 * @param txBodyParser a parser for {@link TransactionBody}
 */
public record SessionContext(
        @NonNull Parser<Query> queryParser,
        @NonNull Parser<Transaction> txParser,
        @NonNull Parser<SignedTransaction> signedParser,
        @NonNull Parser<TransactionBody> txBodyParser) {

    public SessionContext(
            @NonNull final Parser<Query> queryParser,
            @NonNull final Parser<Transaction> txParser,
            @NonNull final Parser<SignedTransaction> signedParser,
            @NonNull final Parser<TransactionBody> txBodyParser) {
        this.queryParser = requireNonNull(queryParser);
        this.txParser = requireNonNull(txParser);
        this.signedParser = requireNonNull(signedParser);
        this.txBodyParser = requireNonNull(txBodyParser);
    }
}
