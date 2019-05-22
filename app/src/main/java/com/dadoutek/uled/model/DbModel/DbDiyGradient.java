package com.dadoutek.uled.model.DbModel;

import android.os.Parcel;
import android.os.Parcelable;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.ToMany;

import java.io.Serializable;
import java.util.List;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.DaoException;
import org.greenrobot.greendao.annotation.Transient;

import com.dadoutek.uled.dao.DaoSession;
import com.dadoutek.uled.dao.DbColorNodeDao;
import com.dadoutek.uled.dao.DbDiyGradientDao;
import com.google.gson.annotations.Expose;

@Entity
public class DbDiyGradient implements Parcelable {

    @Id
    private Long id;
    private String name;
    private int type=0;
    private int speed=50;

    private int index;

    private Long belongRegionId;

    @Expose(serialize = false, deserialize = false)
    @Transient
    public boolean selected;//选择状态

    @ToMany(referencedJoinProperty = "belongDynamicChangeId")
    private List<DbColorNode> colorNodes;

    protected DbDiyGradient(Parcel in) {
        if (in.readByte() == 0) {
            id = null;
        } else {
            id = in.readLong();
        }
        name = in.readString();
        type = in.readInt();
        speed = in.readInt();
    }

    @Generated(hash = 136035107)
    public DbDiyGradient(Long id, String name, int type, int speed, int index, Long belongRegionId) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.speed = speed;
        this.index = index;
        this.belongRegionId = belongRegionId;
    }

    @Generated(hash = 176034366)
    public DbDiyGradient() {
    }

    public static final Creator<DbDiyGradient> CREATOR = new Creator<DbDiyGradient>() {
        @Override
        public DbDiyGradient createFromParcel(Parcel in) {
            return new DbDiyGradient(in);
        }

        @Override
        public DbDiyGradient[] newArray(int size) {
            return new DbDiyGradient[size];
        }
    };
    /** Used to resolve relations */
    @Generated(hash = 2040040024)
    private transient DaoSession daoSession;
    /** Used for active entity operations. */
    @Generated(hash = 1674029410)
    private transient DbDiyGradientDao myDao;

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
        dest.writeInt(type);
        dest.writeInt(speed);
    }

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

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getSpeed() {
        return this.speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public Long getBelongRegionId() {
        return belongRegionId;
    }

    public void setBelongRegionId(Long belongRegionId) {
        this.belongRegionId = belongRegionId;
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
    @Generated(hash = 1558974412)
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getDbDiyGradientDao() : null;
    }

    /**
     * To-many relationship, resolved on first access (and after reset).
     * Changes to to-many relations are not persisted, make changes to the target entity.
     */
    @Generated(hash = 360862439)
    public List<DbColorNode> getColorNodes() {
        if (colorNodes == null) {
            final DaoSession daoSession = this.daoSession;
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            DbColorNodeDao targetDao = daoSession.getDbColorNodeDao();
            List<DbColorNode> colorNodesNew = targetDao._queryDbDiyGradient_ColorNodes(id);
            synchronized (this) {
                if (colorNodes == null) {
                    colorNodes = colorNodesNew;
                }
            }
        }
        return colorNodes;
    }

    /** Resets a to-many relationship, making the next get call to query for a fresh result. */
    @Generated(hash = 596063868)
    public synchronized void resetColorNodes() {
        colorNodes = null;
    }

    public int getIndex() {
        return this.index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
