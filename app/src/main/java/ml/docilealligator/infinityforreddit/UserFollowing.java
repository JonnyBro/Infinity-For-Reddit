package ml.docilealligator.infinityforreddit;

import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import SubscribedUserDatabase.SubscribedUserDao;
import SubscribedUserDatabase.SubscribedUserData;
import User.UserData;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;

class UserFollowing {
    interface UserFollowingListener {
        void onUserFollowingSuccess();
        void onUserFollowingFail();
    }

    static void followUser(Retrofit oauthRetrofit, Retrofit retrofit,
                           String accessToken, String username, String accountName,
                           SubscribedUserDao subscribedUserDao,
                           UserFollowingListener userFollowingListener) {
        userFollowing(oauthRetrofit, retrofit, accessToken, username, accountName, "sub",
                subscribedUserDao, userFollowingListener);
    }

    static void unfollowUser(Retrofit oauthRetrofit, Retrofit retrofit,
                             String accessToken, String username, String accountName,
                             SubscribedUserDao subscribedUserDao,
                             UserFollowingListener userFollowingListener) {
        userFollowing(oauthRetrofit, retrofit, accessToken, username, accountName, "unsub",
                subscribedUserDao, userFollowingListener);
    }

    private static void userFollowing(Retrofit oauthRetrofit, Retrofit retrofit, String accessToken,
                                      String username, String accountName, String action, SubscribedUserDao subscribedUserDao,
                                      UserFollowingListener userFollowingListener) {
        RedditAPI api = oauthRetrofit.create(RedditAPI.class);

        Map<String, String> params = new HashMap<>();
        params.put(RedditUtils.ACTION_KEY, action);
        params.put(RedditUtils.SR_NAME_KEY, "u_" + username);

        Call<String> subredditSubscriptionCall = api.subredditSubscription(RedditUtils.getOAuthHeader(accessToken), params);
        subredditSubscriptionCall.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull retrofit2.Response<String> response) {
                if(response.isSuccessful()) {
                    if(action.equals("sub")) {
                        FetchUserData.fetchUserData(retrofit, username, new FetchUserData.FetchUserDataListener() {
                            @Override
                            public void onFetchUserDataSuccess(UserData userData) {
                                new UpdateSubscriptionAsyncTask(subscribedUserDao, userData, accountName, true).execute();
                            }

                            @Override
                            public void onFetchUserDataFailed() {

                            }
                        });
                    } else {
                        new UpdateSubscriptionAsyncTask(subscribedUserDao, username, accountName, false).execute();
                    }
                    userFollowingListener.onUserFollowingSuccess();
                } else {
                    Log.i("call failed", Integer.toString(response.code()));
                    userFollowingListener.onUserFollowingFail();
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                Log.i("call failed", t.getMessage());
                userFollowingListener.onUserFollowingFail();
            }
        });
    }

    private static class UpdateSubscriptionAsyncTask extends AsyncTask<Void, Void, Void> {

        private SubscribedUserDao subscribedUserDao;
        private String username;
        private String accountName;
        private SubscribedUserData subscribedUserData;
        private boolean isSubscribing;

        UpdateSubscriptionAsyncTask(SubscribedUserDao subscribedUserDao, String username,
                                    String accountName, boolean isSubscribing) {
            this.subscribedUserDao = subscribedUserDao;
            this.username = username;
            this.accountName = accountName;
            this.isSubscribing = isSubscribing;
        }

        UpdateSubscriptionAsyncTask(SubscribedUserDao subscribedUserDao, UserData userData,
                                    String accountName, boolean isSubscribing) {
            this.subscribedUserDao = subscribedUserDao;
            this.subscribedUserData = new SubscribedUserData(userData.getName(), userData.getIconUrl(),
                    userData.getName());
            this.accountName = accountName;
            this.isSubscribing = isSubscribing;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if(isSubscribing) {
                subscribedUserDao.insert(subscribedUserData);
            } else {
                subscribedUserDao.deleteSubscribedUser(username, accountName);
            }
            return null;
        }
    }
}
