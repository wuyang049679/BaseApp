package com.wuyang.baseapp.presenter;

import com.wuyang.baseapp.base.BasePresenterIm;
import com.wuyang.baseapp.base.RxSubscribe;
import com.wuyang.baseapp.contract.MainActivityContract;
import com.wuyang.baseapp.entity.MineEntity;
import com.wuyang.baseapp.model.MainActivityModel;

import io.reactivex.disposables.Disposable;

/**
 * Desccribe:
 *
 * @author Created by wuyang on 2019/9/2
 */
public class MainActivityPresenter extends BasePresenterIm<MainActivityContract.View>implements MainActivityContract.Presenter {

    MainActivityModel mainActivityModel;

    public MainActivityPresenter() {
        this.mainActivityModel=new MainActivityModel();
    }

    @Override
    public void pexample(String version) {

        mainActivityModel.forexample(version).subscribe(new RxSubscribe<MineEntity>() {
            @Override
            protected void onSuccess(MineEntity mineEntity) {
                mView.showContentView();
                mView.showSuccess(mineEntity);
            }

            @Override
            protected void onFailed(int code, String msg) {
                mView.showDataError(msg);
            }

            @Override
            public void onSubscribe(Disposable d) {
                mView.showLoadingView();
                addSubscription(d);
            }
        });
    }
}
