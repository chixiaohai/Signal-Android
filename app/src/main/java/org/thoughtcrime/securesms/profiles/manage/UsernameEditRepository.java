package org.thoughtcrime.securesms.profiles.manage;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.signal.core.util.Result;
import org.signal.core.util.concurrent.SignalExecutors;
import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.database.SignalDatabase;
import org.thoughtcrime.securesms.dependencies.ApplicationDependencies;
import org.thoughtcrime.securesms.jobs.MultiDeviceProfileContentUpdateJob;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.push.exceptions.UsernameIsNotReservedException;
import org.whispersystems.signalservice.api.push.exceptions.UsernameMalformedException;
import org.whispersystems.signalservice.api.push.exceptions.UsernameTakenException;
import org.whispersystems.signalservice.internal.push.ReserveUsernameResponse;

import java.io.IOException;
import java.util.concurrent.Executor;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

class UsernameEditRepository {

  private static final String TAG = Log.tag(UsernameEditRepository.class);

  private final Application                 application;
  private final SignalServiceAccountManager accountManager;
  private final Executor                    executor;

  UsernameEditRepository() {
    this.application    = ApplicationDependencies.getApplication();
    this.accountManager = ApplicationDependencies.getSignalServiceAccountManager();
    this.executor       = SignalExecutors.UNBOUNDED;
  }

  void setUsername(@NonNull String username, @NonNull Callback<UsernameSetResult> callback) {
    executor.execute(() -> callback.onComplete(setUsernameInternal(username)));
  }

  void deleteUsername(@NonNull Callback<UsernameDeleteResult> callback) {
    executor.execute(() -> callback.onComplete(deleteUsernameInternal()));
  }

  @WorkerThread
  private @NonNull UsernameSetResult setUsernameInternal(@NonNull String username) {
    try {
      accountManager.setUsername(username);
      SignalDatabase.recipients().setUsername(Recipient.self().getId(), username);
      Log.i(TAG, "[setUsername] Successfully set username.");
      return UsernameSetResult.SUCCESS;
    } catch (UsernameTakenException e) {
      Log.w(TAG, "[setUsername] Username taken.");
      return UsernameSetResult.USERNAME_UNAVAILABLE;
    } catch (UsernameMalformedException e) {
      Log.w(TAG, "[setUsername] Username malformed.");
      return UsernameSetResult.USERNAME_INVALID;
    } catch (IOException e) {
      Log.w(TAG, "[setUsername] Generic network exception.", e);
      return UsernameSetResult.NETWORK_ERROR;
    }
  }

  @WorkerThread
  private @NonNull UsernameDeleteResult deleteUsernameInternal() {
    try {
      accountManager.deleteUsername();
      SignalDatabase.recipients().setUsername(Recipient.self().getId(), null);
      Log.i(TAG, "[deleteUsername] Successfully deleted the username.");
      return UsernameDeleteResult.SUCCESS;
    } catch (IOException e) {
      Log.w(TAG, "[deleteUsername] Generic network exception.", e);
      return UsernameDeleteResult.NETWORK_ERROR;
    }
  }

  @WorkerThread
  private @NonNull UsernameSetResult confirmUsernameInternal(@NonNull ReserveUsernameResponse reserveUsernameResponse) {
    try {
      accountManager.confirmUsername(reserveUsernameResponse);
      SignalDatabase.recipients().setUsername(Recipient.self().getId(), reserveUsernameResponse.getUsername());
      ApplicationDependencies.getJobManager().add(new MultiDeviceProfileContentUpdateJob());
      Log.i(TAG, "[confirmUsername] Successfully reserved username.");
      return UsernameSetResult.SUCCESS;
    } catch (UsernameTakenException e) {
      Log.w(TAG, "[confirmUsername] Username gone.");
      return UsernameSetResult.USERNAME_UNAVAILABLE;
    } catch (UsernameIsNotReservedException e) {
      Log.w(TAG, "[confirmUsername] Username was not reserved.");
      return UsernameSetResult.USERNAME_INVALID;
    } catch (IOException e) {
      Log.w(TAG, "[confirmUsername] Generic network exception.", e);
      return UsernameSetResult.NETWORK_ERROR;
    }
  }

  enum UsernameSetResult {
    SUCCESS, USERNAME_UNAVAILABLE, USERNAME_INVALID, NETWORK_ERROR
  }

  enum UsernameDeleteResult {
    SUCCESS, NETWORK_ERROR
  }

  interface Callback<E> {
    void onComplete(E result);
  }
}
