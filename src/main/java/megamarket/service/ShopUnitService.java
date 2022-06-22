package megamarket.service;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import megamarket.dto.*;
import megamarket.repository.dao.ShopUnitDao;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ShopUnitService {

    private final ShopUnitDao shopUnitDao;

    @Transactional
    public void importShopUnits(ShopUnitImportRequest request) {

        Set<String> importIds = new HashSet<>(request.items.size());
        Set<String> allParents = new HashSet<>();

        for (ShopUnitImport item : request.items) {
            importIds.add(item.id);
            if (item.parentId != null) {
                allParents.add(item.parentId);
            }
        }

        allParents.addAll(shopUnitDao.getParentIds(importIds));

        shopUnitDao.upsertShopUnits(request.items, request.updateDate);

        if (!allParents.isEmpty()) {
            shopUnitDao.updateAncestorsOfIdsWithDate(allParents, request.updateDate);
        }
    }

    public boolean deleteShopUnits(String id) {
        return shopUnitDao.deleteShopUnits(id);
    }

    @Nullable
    public ShopUnit getShopUnit(String id) {
        List<ShopUnit> shopUnitList = shopUnitDao.getShopUnitWithDescendants(id);

        return flatToNestedShopUnit(shopUnitList, id);
    }

    @Nullable
    private ShopUnit flatToNestedShopUnit(List<ShopUnit> shopUnitList, String id) {
        ShopUnit shopUnitFirst = null;
        Map<String, List<ShopUnit>> map = new HashMap<>();

        for (ShopUnit shopUnit : shopUnitList) {
            if (shopUnit.id.equals(id)) {
                shopUnitFirst = shopUnit;
                continue;
            }
            map.computeIfAbsent(shopUnit.parentId, unit -> new ArrayList<>()).add(shopUnit);
        }

        if (shopUnitFirst == null) {
            return null;
        }

        buildTreeWithPrice(map, shopUnitFirst);

        return shopUnitFirst;
    }

    private AveragePrice buildTreeWithPrice(Map<String, List<ShopUnit>> map, ShopUnit shopUnitParent) {
        if (shopUnitParent.type == ShopUnitType.CATEGORY) {
            List<ShopUnit> children = map.get(shopUnitParent.id);
            if (children != null) { //category with children
                AveragePrice resultAP = new AveragePrice(0L, 0);
                for (ShopUnit child : children) {
                    shopUnitParent.children.add(child);
                    AveragePrice averagePrice = buildTreeWithPrice(map, child);
                    if (averagePrice != null) {
                        resultAP.sum += averagePrice.sum;
                        resultAP.count += averagePrice.count;
                    }
                }
                shopUnitParent.price = resultAP.sum / resultAP.count;
                return resultAP;
            } else { //category without children
                shopUnitParent.price = null;
                return null;
            }
        } else { //offer
            return new AveragePrice(shopUnitParent.price, 1);
        }
    }

    @Nullable
    public ShopUnitStatisticResponse getShopUnitStatistic(LocalDateTime date) {
        return new ShopUnitStatisticResponse(shopUnitDao.getShopUnitStatistic(date));
    }

    @AllArgsConstructor
    private static class AveragePrice {
        Long sum;
        int count;
    };

}
