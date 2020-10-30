package com.dadoutek.uled.dao;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.internal.DaoConfig;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.database.DatabaseStatement;

import com.dadoutek.uled.model.dbModel.DbCurtain;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.
/** 
 * DAO for table "DB_CURTAIN".
*/
public class DbCurtainDao extends AbstractDao<DbCurtain, Long> {

    public static final String TABLENAME = "DB_CURTAIN";

    /**
     * Properties of entity DbCurtain.<br/>
     * Can be used for QueryBuilder and for referencing column names.
     */
    public static class Properties {
        public final static Property Id = new Property(0, Long.class, "id", true, "_id");
        public final static Property MeshAddr = new Property(1, int.class, "meshAddr", false, "MESH_ADDR");
        public final static Property Name = new Property(2, String.class, "name", false, "NAME");
        public final static Property BelongGroupAddr = new Property(3, int.class, "belongGroupAddr", false, "BELONG_GROUP_ADDR");
        public final static Property MacAddr = new Property(4, String.class, "macAddr", false, "MAC_ADDR");
        public final static Property ProductUUID = new Property(5, int.class, "productUUID", false, "PRODUCT_UUID");
        public final static Property Status = new Property(6, int.class, "status", false, "STATUS");
        public final static Property Inverse = new Property(7, boolean.class, "inverse", false, "INVERSE");
        public final static Property ClosePull = new Property(8, boolean.class, "closePull", false, "CLOSE_PULL");
        public final static Property Speed = new Property(9, int.class, "speed", false, "SPEED");
        public final static Property CloseSlowStart = new Property(10, boolean.class, "closeSlowStart", false, "CLOSE_SLOW_START");
        public final static Property Index = new Property(11, int.class, "index", false, "INDEX");
        public final static Property BelongGroupId = new Property(12, Long.class, "belongGroupId", false, "BELONG_GROUP_ID");
        public final static Property GroupName = new Property(13, String.class, "groupName", false, "GROUP_NAME");
        public final static Property Version = new Property(14, String.class, "version", false, "VERSION");
        public final static Property BoundMac = new Property(15, String.class, "boundMac", false, "BOUND_MAC");
        public final static Property BoundMacName = new Property(16, String.class, "boundMacName", false, "BOUND_MAC_NAME");
        public final static Property Rssi = new Property(17, int.class, "rssi", false, "RSSI");
        public final static Property IsSupportOta = new Property(18, boolean.class, "isSupportOta", false, "IS_SUPPORT_OTA");
        public final static Property IsMostNew = new Property(19, boolean.class, "isMostNew", false, "IS_MOST_NEW");
        public final static Property BelongRegionId = new Property(20, int.class, "belongRegionId", false, "BELONG_REGION_ID");
        public final static Property Uid = new Property(21, int.class, "uid", false, "UID");
    }


    public DbCurtainDao(DaoConfig config) {
        super(config);
    }
    
    public DbCurtainDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }

    /** Creates the underlying database table. */
    public static void createTable(Database db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "\"DB_CURTAIN\" (" + //
                "\"_id\" INTEGER PRIMARY KEY AUTOINCREMENT ," + // 0: id
                "\"MESH_ADDR\" INTEGER NOT NULL ," + // 1: meshAddr
                "\"NAME\" TEXT," + // 2: name
                "\"BELONG_GROUP_ADDR\" INTEGER NOT NULL ," + // 3: belongGroupAddr
                "\"MAC_ADDR\" TEXT," + // 4: macAddr
                "\"PRODUCT_UUID\" INTEGER NOT NULL ," + // 5: productUUID
                "\"STATUS\" INTEGER NOT NULL ," + // 6: status
                "\"INVERSE\" INTEGER NOT NULL ," + // 7: inverse
                "\"CLOSE_PULL\" INTEGER NOT NULL ," + // 8: closePull
                "\"SPEED\" INTEGER NOT NULL ," + // 9: speed
                "\"CLOSE_SLOW_START\" INTEGER NOT NULL ," + // 10: closeSlowStart
                "\"INDEX\" INTEGER NOT NULL ," + // 11: index
                "\"BELONG_GROUP_ID\" INTEGER," + // 12: belongGroupId
                "\"GROUP_NAME\" TEXT," + // 13: groupName
                "\"VERSION\" TEXT," + // 14: version
                "\"BOUND_MAC\" TEXT," + // 15: boundMac
                "\"BOUND_MAC_NAME\" TEXT," + // 16: boundMacName
                "\"RSSI\" INTEGER NOT NULL ," + // 17: rssi
                "\"IS_SUPPORT_OTA\" INTEGER NOT NULL ," + // 18: isSupportOta
                "\"IS_MOST_NEW\" INTEGER NOT NULL ," + // 19: isMostNew
                "\"BELONG_REGION_ID\" INTEGER NOT NULL ," + // 20: belongRegionId
                "\"UID\" INTEGER NOT NULL );"); // 21: uid
    }

    /** Drops the underlying database table. */
    public static void dropTable(Database db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "\"DB_CURTAIN\"";
        db.execSQL(sql);
    }

    @Override
    protected final void bindValues(DatabaseStatement stmt, DbCurtain entity) {
        stmt.clearBindings();
 
        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
        stmt.bindLong(2, entity.getMeshAddr());
 
        String name = entity.getName();
        if (name != null) {
            stmt.bindString(3, name);
        }
        stmt.bindLong(4, entity.getBelongGroupAddr());
 
        String macAddr = entity.getMacAddr();
        if (macAddr != null) {
            stmt.bindString(5, macAddr);
        }
        stmt.bindLong(6, entity.getProductUUID());
        stmt.bindLong(7, entity.getStatus());
        stmt.bindLong(8, entity.getInverse() ? 1L: 0L);
        stmt.bindLong(9, entity.getClosePull() ? 1L: 0L);
        stmt.bindLong(10, entity.getSpeed());
        stmt.bindLong(11, entity.getCloseSlowStart() ? 1L: 0L);
        stmt.bindLong(12, entity.getIndex());
 
        Long belongGroupId = entity.getBelongGroupId();
        if (belongGroupId != null) {
            stmt.bindLong(13, belongGroupId);
        }
 
        String groupName = entity.getGroupName();
        if (groupName != null) {
            stmt.bindString(14, groupName);
        }
 
        String version = entity.getVersion();
        if (version != null) {
            stmt.bindString(15, version);
        }
 
        String boundMac = entity.getBoundMac();
        if (boundMac != null) {
            stmt.bindString(16, boundMac);
        }
 
        String boundMacName = entity.getBoundMacName();
        if (boundMacName != null) {
            stmt.bindString(17, boundMacName);
        }
        stmt.bindLong(18, entity.getRssi());
        stmt.bindLong(19, entity.getIsSupportOta() ? 1L: 0L);
        stmt.bindLong(20, entity.getIsMostNew() ? 1L: 0L);
        stmt.bindLong(21, entity.getBelongRegionId());
        stmt.bindLong(22, entity.getUid());
    }

    @Override
    protected final void bindValues(SQLiteStatement stmt, DbCurtain entity) {
        stmt.clearBindings();
 
        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
        stmt.bindLong(2, entity.getMeshAddr());
 
        String name = entity.getName();
        if (name != null) {
            stmt.bindString(3, name);
        }
        stmt.bindLong(4, entity.getBelongGroupAddr());
 
        String macAddr = entity.getMacAddr();
        if (macAddr != null) {
            stmt.bindString(5, macAddr);
        }
        stmt.bindLong(6, entity.getProductUUID());
        stmt.bindLong(7, entity.getStatus());
        stmt.bindLong(8, entity.getInverse() ? 1L: 0L);
        stmt.bindLong(9, entity.getClosePull() ? 1L: 0L);
        stmt.bindLong(10, entity.getSpeed());
        stmt.bindLong(11, entity.getCloseSlowStart() ? 1L: 0L);
        stmt.bindLong(12, entity.getIndex());
 
        Long belongGroupId = entity.getBelongGroupId();
        if (belongGroupId != null) {
            stmt.bindLong(13, belongGroupId);
        }
 
        String groupName = entity.getGroupName();
        if (groupName != null) {
            stmt.bindString(14, groupName);
        }
 
        String version = entity.getVersion();
        if (version != null) {
            stmt.bindString(15, version);
        }
 
        String boundMac = entity.getBoundMac();
        if (boundMac != null) {
            stmt.bindString(16, boundMac);
        }
 
        String boundMacName = entity.getBoundMacName();
        if (boundMacName != null) {
            stmt.bindString(17, boundMacName);
        }
        stmt.bindLong(18, entity.getRssi());
        stmt.bindLong(19, entity.getIsSupportOta() ? 1L: 0L);
        stmt.bindLong(20, entity.getIsMostNew() ? 1L: 0L);
        stmt.bindLong(21, entity.getBelongRegionId());
        stmt.bindLong(22, entity.getUid());
    }

    @Override
    public Long readKey(Cursor cursor, int offset) {
        return cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0);
    }    

    @Override
    public DbCurtain readEntity(Cursor cursor, int offset) {
        DbCurtain entity = new DbCurtain( //
            cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0), // id
            cursor.getInt(offset + 1), // meshAddr
            cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2), // name
            cursor.getInt(offset + 3), // belongGroupAddr
            cursor.isNull(offset + 4) ? null : cursor.getString(offset + 4), // macAddr
            cursor.getInt(offset + 5), // productUUID
            cursor.getInt(offset + 6), // status
            cursor.getShort(offset + 7) != 0, // inverse
            cursor.getShort(offset + 8) != 0, // closePull
            cursor.getInt(offset + 9), // speed
            cursor.getShort(offset + 10) != 0, // closeSlowStart
            cursor.getInt(offset + 11), // index
            cursor.isNull(offset + 12) ? null : cursor.getLong(offset + 12), // belongGroupId
            cursor.isNull(offset + 13) ? null : cursor.getString(offset + 13), // groupName
            cursor.isNull(offset + 14) ? null : cursor.getString(offset + 14), // version
            cursor.isNull(offset + 15) ? null : cursor.getString(offset + 15), // boundMac
            cursor.isNull(offset + 16) ? null : cursor.getString(offset + 16), // boundMacName
            cursor.getInt(offset + 17), // rssi
            cursor.getShort(offset + 18) != 0, // isSupportOta
            cursor.getShort(offset + 19) != 0, // isMostNew
            cursor.getInt(offset + 20), // belongRegionId
            cursor.getInt(offset + 21) // uid
        );
        return entity;
    }
     
    @Override
    public void readEntity(Cursor cursor, DbCurtain entity, int offset) {
        entity.setId(cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0));
        entity.setMeshAddr(cursor.getInt(offset + 1));
        entity.setName(cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2));
        entity.setBelongGroupAddr(cursor.getInt(offset + 3));
        entity.setMacAddr(cursor.isNull(offset + 4) ? null : cursor.getString(offset + 4));
        entity.setProductUUID(cursor.getInt(offset + 5));
        entity.setStatus(cursor.getInt(offset + 6));
        entity.setInverse(cursor.getShort(offset + 7) != 0);
        entity.setClosePull(cursor.getShort(offset + 8) != 0);
        entity.setSpeed(cursor.getInt(offset + 9));
        entity.setCloseSlowStart(cursor.getShort(offset + 10) != 0);
        entity.setIndex(cursor.getInt(offset + 11));
        entity.setBelongGroupId(cursor.isNull(offset + 12) ? null : cursor.getLong(offset + 12));
        entity.setGroupName(cursor.isNull(offset + 13) ? null : cursor.getString(offset + 13));
        entity.setVersion(cursor.isNull(offset + 14) ? null : cursor.getString(offset + 14));
        entity.setBoundMac(cursor.isNull(offset + 15) ? null : cursor.getString(offset + 15));
        entity.setBoundMacName(cursor.isNull(offset + 16) ? null : cursor.getString(offset + 16));
        entity.setRssi(cursor.getInt(offset + 17));
        entity.setIsSupportOta(cursor.getShort(offset + 18) != 0);
        entity.setIsMostNew(cursor.getShort(offset + 19) != 0);
        entity.setBelongRegionId(cursor.getInt(offset + 20));
        entity.setUid(cursor.getInt(offset + 21));
     }
    
    @Override
    protected final Long updateKeyAfterInsert(DbCurtain entity, long rowId) {
        entity.setId(rowId);
        return rowId;
    }
    
    @Override
    public Long getKey(DbCurtain entity) {
        if(entity != null) {
            return entity.getId();
        } else {
            return null;
        }
    }

    @Override
    public boolean hasKey(DbCurtain entity) {
        return entity.getId() != null;
    }

    @Override
    protected final boolean isEntityUpdateable() {
        return true;
    }
    
}
