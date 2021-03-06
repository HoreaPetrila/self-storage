/**
 * Copyright (c) 2020, Self XDSD Contributors
 * All rights reserved.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"),
 * to read the Software only. Permission is hereby NOT GRANTED to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.selfxdsd.storage;

import com.selfxdsd.api.*;
import com.selfxdsd.api.storage.Storage;
import com.selfxdsd.core.contracts.invoices.ContractInvoices;
import com.selfxdsd.core.contracts.invoices.StoredInvoice;
import org.jooq.Record;
import org.jooq.Result;

import java.util.Iterator;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.selfxdsd.storage.generated.jooq.Tables.SLF_INVOICES_XDSD;

/**
 * Invoices in Self.
 * @author Mihai Andronache (amihaiemil@gmail.com)
 * @version $Id$
 * @since 0.0.4
 * @todo #101:30min Implement and test boolean::registerAsPaid(Invoice) for
 *  SelfInvoices.
 */
public final class SelfInvoices implements Invoices {

    /**
     * Parent Storage.
     */
    private final Storage storage;

    /**
     * Database.
     */
    private final Database database;

    /**
     * Ctor.
     * @param storage Parent Storage.
     * @param database Database.
     */
    public SelfInvoices(
        final Storage storage,
        final Database database
    ) {
        this.storage = storage;
        this.database = database;
    }

    @Override
    public Invoice getById(final int id) {
        final Result<Record> result = this.database.jooq()
            .select()
            .from(SLF_INVOICES_XDSD)
            .where(SLF_INVOICES_XDSD.INVOICEID.eq(id))
            .fetch();
        if(!result.isEmpty()) {
            return this.buildInvoice(result.get(0));
        }
        return null;
    }

    @Override
    public Invoice createNewInvoice(final Contract.Id contractId) {
        throw new UnsupportedOperationException(
            "Cannot create an Invoice outside of a Contract. "
          + "Call #ofContract(...) first."
        );
    }

    @Override
    public Invoice active() {
        throw new UnsupportedOperationException(
            "Cannot get the active Invoice out of all of them. "
          + "Call #ofContract(...) first."
        );
    }

    @Override
    public Invoices ofContract(final Contract.Id id) {
        final Contract contract = this.storage.contracts().findById(id);
        final Supplier<Stream<Invoice>> ofContract = () -> this.database.jooq()
            .select()
            .from(SLF_INVOICES_XDSD)
            .where(SLF_INVOICES_XDSD.REPO_FULLNAME.eq(id.getRepoFullName())
                .and(SLF_INVOICES_XDSD.USERNAME.eq(id.getContributorUsername()))
                .and(SLF_INVOICES_XDSD.PROVIDER.eq(id.getProvider()))
                .and(SLF_INVOICES_XDSD.ROLE.eq(id.getRole()))
            )
            .stream()
            .map(record -> buildInvoice(record, contract));
        return new ContractInvoices(id, ofContract, this.storage);
    }

    @Override
    public boolean registerAsPaid(final Invoice invoice) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Iterator<Invoice> iterator() {
        throw new UnsupportedOperationException(
            "You cannot iterate over all Invoices. "
          + "Call #ofContract(...) first."
        );
    }

    /**
     * Builds an Invoice from a {@link Record}.
     * @param record Record.
     * @return Invoice.
     */
    private Invoice buildInvoice(final Record record) {
        final Contract contract = this.storage.contracts().findById(
            new Contract.Id(
                record.getValue(SLF_INVOICES_XDSD.REPO_FULLNAME),
                record.getValue(SLF_INVOICES_XDSD.USERNAME),
                record.getValue(SLF_INVOICES_XDSD.PROVIDER),
                record.getValue(SLF_INVOICES_XDSD.ROLE)
            )
        );
        return this.buildInvoice(record, contract);
    }

    /**
     * Builds an Invoice from a {@link Record} and a {@link Contract}.
     *
     * @param record Record.
     * @param contract Contract.
     * @return Invoice.
     */
    private Invoice buildInvoice(final Record record, final Contract contract) {
        return new StoredInvoice(
            record.getValue(SLF_INVOICES_XDSD.INVOICEID),
            contract,
            record.getValue(SLF_INVOICES_XDSD.CREATEDAT),
            record.getValue(SLF_INVOICES_XDSD.PAYMENT_TIMESTAMP),
            record.getValue(SLF_INVOICES_XDSD.TRANSACTIONID),
            this.storage
        );
    }

}
