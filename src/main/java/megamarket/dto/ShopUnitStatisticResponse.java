package megamarket.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class ShopUnitStatisticResponse {

    @NotNull
    public final List<ShopUnitStatisticUnit> items;
}
