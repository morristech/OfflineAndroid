package com.peterdang.offlineandroid.domain.services;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;

import com.peterdang.offlineandroid.domain.usecases.DeleteCommentUseCase;
import com.peterdang.offlineandroid.domain.usecases.UpdateCommentUseCase;
import com.peterdang.offlineandroid.models.Comment;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Updates local database after remote comment sync requests
 */
public class SyncCommentLifecycleObserver implements LifecycleObserver {
    private final UpdateCommentUseCase updateCommentUseCase;
    private final DeleteCommentUseCase deleteCommentUseCase;
    private final CompositeDisposable disposables = new CompositeDisposable();

    public SyncCommentLifecycleObserver(UpdateCommentUseCase updateCommentUseCase,
                                        DeleteCommentUseCase deleteCommentUseCase) {
        this.updateCommentUseCase = updateCommentUseCase;
        this.deleteCommentUseCase = deleteCommentUseCase;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        Timber.d("onResume lifecycle event.");
        disposables.add(SyncCommentRxBus.getInstance().toObservable()
                .subscribe(this::handleSyncResponse, t -> Timber.e(t, "error handling sync response")));
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        Timber.d("onPause lifecycle event.");
        disposables.clear();
    }

    private void handleSyncResponse(SyncCommentResponse response) {
        if (response.eventType == SyncResponseEventType.SUCCESS) {
            onSyncCommentSuccess(response.comment);
        } else {
            onSyncCommentFailed(response.comment);
        }
    }

    private void onSyncCommentSuccess(Comment comment) {
        Timber.d("received sync comment success event for comment %s", comment);
        disposables.add(updateCommentUseCase.updateComment(comment)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> Timber.d("updateComment comment success"),
                        t -> Timber.e(t, "updateComment comment error")));
    }

    private void onSyncCommentFailed(Comment comment) {
        Timber.d("received sync comment failed event for comment %s", comment);
        disposables.add(deleteCommentUseCase.deleteComment(comment)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> Timber.d("delete comment success"),
                        t -> Timber.e(t, "delete comment error")));
    }
}
