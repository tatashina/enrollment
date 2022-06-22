package megamarket.dto;

import lombok.Data;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class ShopUnitStatisticUnit {

    @NotNull
    public final String id;
    @NotNull
    public final String name;
    @Nullable
    public final String parentId;
    @NotNull
    public final ShopUnitType type;
    @Nullable
    public final Long price;
    @NotNull
    public final LocalDateTime date;
}
