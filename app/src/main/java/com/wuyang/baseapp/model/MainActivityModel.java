package com.wuyang.baseapp.model;


import com.wuyang.baseapp.api.ApiManager;
import com.wuyang.baseapp.base.BaseEntity;
import com.wuyang.baseapp.contract.MainActivityContract;
import com.wuyang.baseapp.entity.MineEntity;
import com.wuyang.baseapp.utils.RequestBodyBuilder;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.RequestBody;


/**
 * Desccribe:
 *
 * @author Created by wuyang on 2019/9/2
 */
public class MainActivityModel implements MainActivityContract.Model {

    @Override
    public Observable<BaseEntity<MineEntity>> forexample(String version) {
        RequestBodyBuilder.Builder builder = new RequestBodyBuilder.Builder();
        RequestBody requestBody = builder
                .params("version",version)
                .build();

        return ApiManager.getApistore().example(requestBody).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }
}
