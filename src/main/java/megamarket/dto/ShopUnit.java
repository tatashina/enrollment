package megamarket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class ShopUnit {

        @NotNull
        public final String id;
        @NotNull
        public final String name;
        @NotNull
        public final LocalDateTime date;
        @Nullable
        public final String parentId;
        @NotNull
        public final ShopUnitType type;
        @Nullable
        public Long price;
        @Nullable
        public final List<ShopUnit> children;
}
