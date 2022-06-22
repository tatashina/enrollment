package megamarket.repository.dao;

import lombok.RequiredArgsConstructor;
import megamarket.dto.*;
import megamarket.utils.ResultSetReader;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;

@Repository
@RequiredArgsConstructor
public class ShopUnitDao {

    private static final String UPSERT_SHOPUNITS = """
            INSERT INTO shopunit (id, name, type, price, date, parentid)
            VALUES (:id, :name, :type, :price, :date, :parentId)
            ON CONFLICT (id) DO UPDATE SET
                name = excluded.name,
                type = excluded.type,
                price = excluded.price,
                date = excluded.date,
                parentid = excluded.parentid;
            """;

    private static final String UPDATE_ANCESTORS_OF_IDS_WITH_DATE = """
            UPDATE shopunit
                SET date = CASE WHEN date < :date then :date ELSE date END
                FROM (
                    WITH RECURSIVE tree as (
                       SELECT id, parentid from shopunit where id in (:ids)
                       union all
                       select c.id, c.parentid
                       from shopunit c
                         join tree p on p.parentid = c.id
                    )
                    select id from tree GROUP BY id
                ) ids
                WHERE shopunit.id = ids.id;
            """;

    private static final String SELECT_PARENTIDS_BY_IDS =
            "SELECT parentId FROM shopunit WHERE id IN (:ids) AND parentId is not null;";

    private static final String GET_DESCENDANTS_TREE_RECURSIVE = """
            WITH RECURSIVE tree AS (
                SELECT id, name, type, price, date, parentId,  ARRAY[]::text[] AS ancestors FROM shopunit WHERE parentid = :id
                UNION ALL
                SELECT shopunit.id, shopunit.name, shopunit.type, shopunit.price, shopunit.date, shopunit.parentId, tree.ancestors || shopunit.parentid
                FROM shopunit, tree
                WHERE shopunit.parentid = tree.id
            )
            """;

    private static final String SELECT_SHOPUNIT_WITH_DESCENDANTS_BY_ID =
            GET_DESCENDANTS_TREE_RECURSIVE + """
                SELECT id, name, type, price, date, parentId FROM tree
                UNION
                SELECT id, name, type, price, date, parentId FROM shopunit WHERE id = :id;
                """;

    private static final String DELETE_SHOPUNIT_BY_ID =
            GET_DESCENDANTS_TREE_RECURSIVE + """
                DELETE FROM shopunit
                    WHERE id IN (SELECT id FROM tree) OR id = :id;
                """;


    private static final String GET_OFFERS_BY_TIME = """
            SELECT id, name, type, price, date, parentId FROM shopunit
            WHERE type = :type and date >= :date - interval '24 hours';
            """;


    private final NamedParameterJdbcTemplate jdbcTemplate;


    public void upsertShopUnits(List<ShopUnitImport> shopUnits, LocalDateTime updateDate) {
        MapSqlParameterSource[] batchArgs = new MapSqlParameterSource[shopUnits.size()];
        for (int i = 0; i < shopUnits.size(); i++) {
            ShopUnitImport shopUnit = shopUnits.get(i);
            MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource()
                    .addValue("id", shopUnit.id)
                    .addValue("name", shopUnit.name)
                    .addValue("type", shopUnit.type.name())
                    .addValue("price", shopUnit.price)
                    .addValue("date", updateDate)
                    .addValue("parentId", shopUnit.parentId);
            batchArgs[i] = mapSqlParameterSource;
        }

        jdbcTemplate.batchUpdate(UPSERT_SHOPUNITS, batchArgs);

    }

    public void updateAncestorsOfIdsWithDate(Set<String> ids, LocalDateTime dateTime) {
        MapSqlParameterSource[] batchArgs = new MapSqlParameterSource[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource()
                    .addValue("ids", ids)
                    .addValue("date", dateTime);
            batchArgs[i] = mapSqlParameterSource;
        }

        jdbcTemplate.batchUpdate(UPDATE_ANCESTORS_OF_IDS_WITH_DATE, batchArgs);
    }


    public List<ShopUnit> getShopUnitWithDescendants(String id) {
        return jdbcTemplate.query(
                SELECT_SHOPUNIT_WITH_DESCENDANTS_BY_ID,
                new MapSqlParameterSource("id", id),
                rs -> {
                    ResultSetReader rsr = new ResultSetReader(rs);
                    List<ShopUnit> result = new ArrayList<>();
                    while (rsr.next()) {
                        result.add(buildShopUnit(rsr));
                    }
                    return result;
                }
        );
    }

    public boolean deleteShopUnits(String id) {
        int deleted = jdbcTemplate.update(
                DELETE_SHOPUNIT_BY_ID,
                new MapSqlParameterSource("id", id)
        );
        return deleted > 0;
    }

    public Set<String> getParentIds(Set<String> ids) {
        return jdbcTemplate.query(
                SELECT_PARENTIDS_BY_IDS,
                new MapSqlParameterSource("ids", ids),
                rs -> {
                    ResultSetReader rsr = new ResultSetReader(rs);
                    Set<String> result = new HashSet<>();
                    while (rsr.next()) {
                        String parentId = rsr.readStringNullable("parentId");
                        if (parentId != null) {
                            result.add(parentId);
                        }
                    }
                    return result;
                }
        );
    }


    public List<ShopUnitStatisticUnit> getShopUnitStatistic(LocalDateTime date) {
        return jdbcTemplate.query(
                GET_OFFERS_BY_TIME,
                new MapSqlParameterSource()
                        .addValue("type", ShopUnitType.OFFER.name())
                        .addValue("date", date),
                rs -> {
                    ResultSetReader rsr = new ResultSetReader(rs);
                    List<ShopUnitStatisticUnit> result = new ArrayList<>();
                    while (rsr.next()) {
                        result.add(buildShopUnitStatisticUnit(rsr));
                    }
                    return result;
                }
        );
    }


    private ShopUnit buildShopUnit(ResultSetReader rsr) throws SQLException {
        ShopUnitType type = ShopUnitType.valueOf(requireNonNull(rsr.readStringNullable("type")));
        Long price = rsr.readLongNullable("price");

        return new ShopUnit(
                requireNonNull(rsr.readStringNullable("id")),
                requireNonNull(rsr.readStringNullable("name")),
                requireNonNull(rsr.readTimestampNullable("date")).toLocalDateTime(),
                rsr.readStringNullable("parentId"),
                type,
                price == null ? 0 : price,
                type == ShopUnitType.OFFER ? null : new ArrayList<>()
        );
    }

    private ShopUnitStatisticUnit buildShopUnitStatisticUnit(ResultSetReader rsr) throws SQLException {
        return new ShopUnitStatisticUnit(
                requireNonNull(rsr.readStringNullable("id")),
                requireNonNull(rsr.readStringNullable("name")),
                rsr.readStringNullable("parentId"),
                ShopUnitType.valueOf(requireNonNull(rsr.readStringNullable("type"))),
                rsr.readLongNullable("price"),
                requireNonNull(rsr.readTimestampNullable("date")).toLocalDateTime()
        );
    }
}
