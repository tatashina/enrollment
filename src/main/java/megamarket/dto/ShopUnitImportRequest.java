package megamarket.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ShopUnitImportRequest {

    @NotNull
    @NotEmpty
    public final List<@Valid ShopUnitImport> items;

    @NotNull
    public final LocalDateTime updateDate;
}
