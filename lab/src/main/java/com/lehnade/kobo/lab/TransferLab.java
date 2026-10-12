package com.lehnade.kobo.lab;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TransferLab {


    public static void main(String[] args) throws SQLException, InterruptedException {

        List<TransferStrategy> strategies = new ArrayList<>();
        strategies.add(new AtomicTransfer());
        strategies.add(new OptimisticTransfer());
        strategies.add(new PessimisticOrderedTransfer());
        strategies.add(new PessimisticUnorderedTransfer());
        strategies.add(new SerializableTransfer());
        TransferRunner.run(args, strategies);

    }



}
