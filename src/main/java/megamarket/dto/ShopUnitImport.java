package megamarket.dto;

import lombok.Data;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;

@Data
public class ShopUnitImport {

    @NotNull
    public final String id;
    @NotNull
    public final String name;
    @Nullable
    public final String parentId;
    @NotNull
    public final ShopUnitType type;
    @Nullable
    @PositiveOrZero
    public final Long price;
}