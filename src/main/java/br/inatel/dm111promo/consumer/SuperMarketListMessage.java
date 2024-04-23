package br.inatel.dm111promo.consumer;

import java.util.List;

public record SuperMarketListMessage(
        String id,
        String name,
        String userId,
        List<String>products,
        long lastUpdatedOn
) {
}
