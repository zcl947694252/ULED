package com.dadoutek.uled.model.DbModel;

import android.os.Parcel;
import android.os.Parcelable;

import com.dadoutek.uled.dao.DaoSession;
import com.dadoutek.uled.dao.DbSceneActionsDao;
import com.dadoutek.uled.dao.DbSceneDao;

import org.greenrobot.greendao.DaoException;
import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.ToMany;

import java.io.Serializable;
import java.util.List;

/**
 * Created by hejiajun on 2018/5/5.
 */

@Entity
public class DbScene implements Parcelable{

    @Id(autoincrement = true)
    private Long id;

    private String name;

    @NotNull
    private Long belongRegionId;

    @ToMany(referencedJoinProperty = "actionId")
    private List<DbSceneActions> actions;

    /** Used to resolve relations */
    @Generated(hash = 2040040024)
    private transient DaoSession daoSession;

    /** Used for active entity operations. */
    @Generated(hash = 1485122994)
    private transient DbSceneDao myDao;

    @Generated(hash = 1820180226)
    public DbScene(Long id, String name, @NotNull Long belongRegionId) {
        this.id = id;
        this.name = name;
        this.belongRegionId = belongRegionId;
    }

    @Generated(hash = 662958756)
    public DbScene() {
    }

    protected DbScene(Parcel in) {
        if (in.readByte() == 0) {
            id = null;
        } else {
            id = in.readLong();
        }
        name = in.readString();
        if (in.readByte() == 0) {
            belongRegionId = null;
        } else {
            belongRegionId = in.readLong();
        }
    }

    public static final Creator<DbScene> CREATOR = new Creator<DbScene>() {
        @Override
        public DbScene createFromParcel(Parcel in) {
            return new DbScene(in);
        }

        @Override
        public DbScene[] newArray(int size) {
            return new DbScene[size];
        }
    };

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getBelongRegionId() {
        return this.belongRegionId;
    }

    public void setBelongRegionId(Long belongRegionId) {
        this.belongRegionId = belongRegionId;
    }

    /**
     * To-many relationship, resolved on first access (and after reset).
     * Changes to to-many relations are not persisted, make changes to the target entity.
     */
    @Generated(hash = 1787079285)
    public List<DbSceneActions> getActions() {
        if (actions == null) {
            final DaoSession daoSession = this.daoSession;
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            DbSceneActionsDao targetDao = daoSession.getDbSceneActionsDao();
            List<DbSceneActions> actionsNew = targetDao._queryDbScene_Actions(id);
            synchronized (this) {
                if (actions == null) {
                    actions = actionsNew;
                }
            }
        }
        return actions;
    }

    /** Resets a to-many relationship, making the next get call to query for a fresh result. */
    @Generated(hash = 1155922067)
    public synchronized void resetActions() {
        actions = null;
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#delete(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 128553479)
    public void delete() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.delete(this);
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#refresh(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 1942392019)
    public void refresh() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.refresh(this);
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#update(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 713229351)
    public void update() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.update(this);
    }

    /** called by internal mechanisms, do not call yourself. */
    @Generated(hash = 1398896232)
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getDbSceneDao() : null;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (id == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(id);
        }
        dest.writeString(name);
        if (belongRegionId == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(belongRegionId);
        }
    }
}