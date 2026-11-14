package com.lehnade.kobo.application;

import com.lehnade.kobo.domain.TransferId;
import com.lehnade.kobo.domain.TransferRejection;

public sealed interface TransferOutcome {

    record Completed(TransferId id) implements  TransferOutcome {}
    record Rejected(TransferRejection rejection) implements  TransferOutcome {}
    record Processing() implements  TransferOutcome {}
    record KeyReused() implements  TransferOutcome {}
}
