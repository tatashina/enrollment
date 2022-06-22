package megamarket.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import megamarket.dto.ShopUnit;
import megamarket.dto.ShopUnitImportRequest;
import megamarket.exception.ShopUnitNotFoundException;
import megamarket.service.ShopUnitService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Api
@RestController
@RequiredArgsConstructor
public class ShopUnitController {

    private final ShopUnitService shopUnitService;

    @ApiOperation("Импортирует новые товары и/или категории.")
    @PostMapping("/imports")
    public ResponseEntity<String> importShopUnits(
            @Valid
            @RequestBody
                    ShopUnitImportRequest request
    ) {
        shopUnitService.importShopUnits(request);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation("Удалить элемент по идентификатору.")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteShopUnit(
            @NotEmpty
            @PathVariable
                    String id
    ) {
        if (!shopUnitService.deleteShopUnits(id)) {
            throw new ShopUnitNotFoundException();
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation("Получить информацию об элементе по идентификатору.")
    @GetMapping("/nodes/{id}")
    public ResponseEntity<ShopUnit> getShopUnit(
            @NotEmpty
            @NotNull
            @PathVariable
                    String id
    ) {

        ShopUnit shopUnit = shopUnitService.getShopUnit(id);

        if (shopUnit == null) {
            throw new ShopUnitNotFoundException();
        }

        return new ResponseEntity<>(shopUnit, HttpStatus.OK);
    }
}