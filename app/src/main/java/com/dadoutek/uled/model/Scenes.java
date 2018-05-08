package com.dadoutek.uled.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by hejiajun on 2018/5/2.
 */

public class Scenes implements Serializable{
    //应用到场景的组mesh
    public List<Integer> action=new ArrayList<>();
    //场景名
    public String sceneName;
}
