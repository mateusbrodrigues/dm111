package br.inatel.dm111promo.api.supermaketlist.service;

import br.inatel.dm111promo.api.core.ApiException;
import br.inatel.dm111promo.api.core.AppErrorCode;
import br.inatel.dm111promo.api.supermaketlist.SuperMarketListRequest;

import br.inatel.dm111promo.persistence.product.ProductRepository;
import br.inatel.dm111promo.persistence.promo.PromoFirebaseRepository;
import br.inatel.dm111promo.persistence.supermarketlist.SuperMarketList;
import br.inatel.dm111promo.persistence.supermarketlist.SuperMarketListRepository;
import br.inatel.dm111promo.persistence.user.UserFirebaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
public class SuperMarketListService {

    private static final Logger log = LoggerFactory.getLogger(SuperMarketListService.class);

    private final SuperMarketListRepository splRepository;
    private final ProductRepository productRepository;
    private final UserFirebaseRepository userRepository;

    private final PromoFirebaseRepository promoRepository;

    public SuperMarketListService(SuperMarketListRepository splRepository, ProductRepository productRepository, UserFirebaseRepository userRepository, PromoFirebaseRepository promoRepository) {
        this.splRepository = splRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.promoRepository = promoRepository;
    }

    public List<SuperMarketList> searchAllLists(String userId) throws ApiException {
        try {
            return splRepository.findAllByUserId(userId);
        } catch (ExecutionException | InterruptedException e) {
            log.error(e.getMessage(), e);
            throw new ApiException(AppErrorCode.SUPERMARKET_LIST_QUERY_ERROR);
        }
    }

    public SuperMarketList searchById(String userId, String id) throws ApiException {
        return retrieveSuperMarketList(userId, id);
    }

    public SuperMarketList createList(String userId, SuperMarketListRequest request) throws ApiException {
        validateUser(userId);
        var list = buildSuperMarketList(userId, request);

        var allProductsAvailable = true;
        for (String id: list.getProducts()) {
            try {
                if (productRepository.findById(id).isEmpty()) {
                    allProductsAvailable = false;
                    break;
                }
            } catch (ExecutionException | InterruptedException e) {
                throw new ApiException(AppErrorCode.PRODUCTS_QUERY_ERROR);
            }
        }

        if (allProductsAvailable) {
            splRepository.save(list);
            return list;
        } else {
            throw new ApiException(AppErrorCode.PRODUCTS_NOT_FOUND);
        }
    }

    public SuperMarketList updateList(String userId, String id, SuperMarketListRequest request) throws ApiException {
        validateUser(userId);
        var list = retrieveSuperMarketList(userId, id);
        list.setName(request.name());
        list.setProducts(request.products());

        if (!list.getUserId().equals(userId)) {
            throw new ApiException(AppErrorCode.SUPERMARKET_LIST_OPERATION_NOT_ALLOWED);
        }

        var allProductsAvailable = true;
        for (String productId: list.getProducts()) {
            try {
                if (productRepository.findById(productId).isEmpty()) {
                    allProductsAvailable = false;
                    break;
                }
            } catch (ExecutionException | InterruptedException e) {
                throw new ApiException(AppErrorCode.PRODUCTS_QUERY_ERROR);
            }
        }

        if (allProductsAvailable) {
            splRepository.update(list);

            return list;
        } else {
            throw new ApiException(AppErrorCode.PRODUCTS_NOT_FOUND);
        }

    }

    public void removeList(String userId, String id) throws ApiException {
        try {
            var splOpt = splRepository.findByUserIdAndId(userId, id);
            if (splOpt.isPresent()) {
                var spl = splOpt.get();
                splRepository.delete(spl.getId());
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new ApiException(AppErrorCode.SUPERMARKET_LIST_QUERY_ERROR);
        }
    }

    private void validateUser(String userId) throws ApiException {
        try {
            userRepository.findById(userId)
                    .orElseThrow(() -> new ApiException(AppErrorCode.USER_NOT_FOUND));
        } catch (ExecutionException | InterruptedException e) {
            throw new ApiException(AppErrorCode.USERS_QUERY_ERROR);
        }
    }

    private SuperMarketList buildSuperMarketList(String userId, SuperMarketListRequest request) {
        var id = UUID.randomUUID().toString();
        return new SuperMarketList(id, request.name(), userId, request.products());
    }

    private SuperMarketList retrieveSuperMarketList(String userId, String id) throws ApiException {
        try {
            return splRepository.findByUserIdAndId(userId, id)
                    .orElseThrow(() -> new ApiException(AppErrorCode.SUPERMARKET_LIST_NOT_FOUND));
        } catch (ExecutionException | InterruptedException e) {
            throw new ApiException(AppErrorCode.SUPERMARKET_LIST_QUERY_ERROR);
        }
    }

   /* public List<String> getProductsForUser(String userId) throws ApiException {
        List<String> productIdsForUser = new ArrayList<>();
        try {
            List<SuperMarketList> lists = splRepository.findAllByUserId(userId);
            for (SuperMarketList list : lists) {
                for (String productId : list.getProducts()) {
                    if (isValidProductForPromotion(productId)) {
                        productIdsForUser.add(productId);
                    }
                }
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new ApiException(AppErrorCode.SUPERMARKET_LIST_QUERY_ERROR);
        }
        return productIdsForUser;
    }*/

    /*private boolean isValidProductForPromotion(String productId) throws ApiException {
        try {
            // Check if the product exists in the promotions collection
            return promoRepository.existsByProductId(productId).get();
        } catch (ExecutionException | InterruptedException e) {
            throw new ApiException(AppErrorCode.PROMO_QUERY_ERROR);
        }
    }*/
}
