package megamarket.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import megamarket.dto.ShopUnitStatisticResponse;
import megamarket.service.ShopUnitService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Api
@RestController
@RequiredArgsConstructor
public class StatisticController {

    private final ShopUnitService shopUnitService;

    @ApiOperation("Получение списка товаров, цена которых была обновлена за последние 24 часа от date")
    @GetMapping("/sales")
    public ResponseEntity<ShopUnitStatisticResponse> getShopUnit(
            @NotNull
            @RequestParam
                    LocalDateTime date
    ) {

        ShopUnitStatisticResponse shopUnitStatistic = shopUnitService.getShopUnitStatistic(date);

        return new ResponseEntity<>(shopUnitStatistic, HttpStatus.OK);
    }
}
