package com.lehnade.kobo.lab;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TransferLab {


    public static void main(String[] args) throws SQLException, InterruptedException {

        TransferStrategy strategy = switch (args.length > 0 ? args[0] : "naive") {
            case "atomic" -> new AtomicTransfer();
            case "optimistic" -> new OptimisticTransfer();
            case "pessimistic" -> new PessimisticOrderedTransfer();
            case "pessimistic-unordered" -> new PessimisticUnorderedTransfer();
            case "serializable" -> new SerializableTransfer();
            default -> new NaiveTransfer();
        };
        List<TransferStrategy> strategies = new ArrayList<>();
        strategies.add(new AtomicTransfer());
        strategies.add(new OptimisticTransfer());
        strategies.add(new PessimisticOrderedTransfer());
        strategies.add(new PessimisticUnorderedTransfer());
        strategies.add(new SerializableTransfer());
        TransferRunner.run(args, strategies);

    }



}
