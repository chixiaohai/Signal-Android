package org.thoughtcrime.securesms.conversationlist;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import org.signal.core.util.Stopwatch;
import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.conversationlist.ConversationListItemOptionsAdapter.ItemClickListener;
import org.thoughtcrime.securesms.conversationlist.model.Conversation;
import org.thoughtcrime.securesms.database.RecipientTable;
import org.thoughtcrime.securesms.database.SignalDatabase;
import org.thoughtcrime.securesms.database.ThreadTable;
import org.thoughtcrime.securesms.dependencies.ApplicationDependencies;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.AppStartup;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ConversationListItemOptionsFragment extends ConversationListFragment
        implements ItemClickListener{

  private static final String                             TAG = Log.tag(ConversationListItemOptionsFragment.class);
  private              ThreadTable                        threadDatabase;
  private              ConversationListItemOptionsAdapter defaultAdapter;
  private RecyclerView list;
  private long threadId;
  private Set<Long> deleteConversationSet;
//    private ConversationListItemOptionsAdapter optionsAdapter;
  private Stopwatch startupStopwatch;

  public static ConversationListItemOptionsFragment newInstance() {
    return new ConversationListItemOptionsFragment();
  }

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    startupStopwatch = new Stopwatch("startup");
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
//    Log.i(TAG,"onViewCreated");
    threadId = getNavigator().getPreDeleteThreadId();
    deleteConversationSet = getNavigator().getPreDeleteSet();
    list = view.findViewById(R.id.list);
    defaultAdapter = new ConversationListItemOptionsAdapter(requireContext(), null, this);
    threadDatabase = SignalDatabase.threads();
    defaultAdapter.setData(getDatas());
    list.setAdapter(defaultAdapter);
    defaultAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
      @Override
      public void onItemRangeInserted(int positionStart, int itemCount) {
        startupStopwatch.split("data-set");
        defaultAdapter.unregisterAdapterDataObserver(this);
        list.post(() -> {
          AppStartup.getInstance().onCriticalRenderEventEnd();
          startupStopwatch.split("first-render");
          startupStopwatch.stop(TAG);
        });
      }
    });
  }

  @Override
  public void onResume() {
    super.onResume();

    list.getAdapter().notifyDataSetChanged();

    if (list.getAdapter() != defaultAdapter) {
      list.setAdapter(defaultAdapter);
    }
    list.requestFocus();
  }

  private List<String> getDatas() {
    List<String> datas = null;

    if (threadDatabase.isArchived(threadDatabase.getRecipientIdForThreadId(threadId))) {
      datas = Arrays.asList(getResources().getStringArray(R.array.conversation_list_item_archive_options_menu));
    } else {
      datas = Arrays.asList(getResources().getStringArray(R.array.conversation_list_item_options_menu));
    }

    return datas;
  }

  @Override
  public void onItemClick(ConversationListItem item) {
    RecipientTable.RegisteredState registeredState = Recipient.self().resolve().getRegistered();
    if(registeredState == RecipientTable.RegisteredState.UNKNOWN){
      return;
    }
    String text = item.getFromText();
    if (text.equals(requireContext().getResources().getString(R.string.conversation_list_batch_archive__menu_archive_selected))) {
      archiveThread();
    } else if (text.equals(requireContext().getResources().getString(R.string.conversation_list_batch__menu_delete_selected))) {
      handleDeleteAllSelected();
    }else if(text.equals(requireContext().getResources().getString(R.string.conversation_list_batch_unarchive__menu_unarchive_selected))){
      unArchiveThread();
    }
  }

  @SuppressLint({ "StaticFieldLeak", "WrongThread" })
  private void archiveThread() {
    new AsyncTask<Void, Void, Void>() {

      @Override
      protected void onPreExecute() {
      }

      @Override
      protected Void doInBackground(Void... params) {
        SignalDatabase.threads().archiveConversation(threadId);
        return null;
      }

      @Override
      protected void onPostExecute(Void result) {
        backToConversationListFragment();
      }
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

  }

  @SuppressLint({ "StaticFieldLeak", "WrongThread" })
  private void unArchiveThread() {
    new AsyncTask<Void, Void, Void>() {

      @Override
      protected void onPreExecute() {
      }


      @Override
      protected Void doInBackground(Void... params) {
        SignalDatabase.threads().unarchiveConversation(threadId);
        return null;
      }

      @Override
      protected void onPostExecute(Void result) {
        backToConversationListFragment();
      }
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

  }

  @SuppressLint("StaticFieldLeak")
  private void handleDeleteAllSelected() {
    int                 conversationsCount = deleteConversationSet.size();
    AlertDialog.Builder alert              = new AlertDialog.Builder(getActivity());
    alert.setIcon(R.drawable.ic_warning);
    alert.setTitle(getActivity().getResources().getQuantityString(R.plurals.ConversationListFragment_delete_selected_conversations,
                                                                  conversationsCount, conversationsCount));
    alert.setMessage(getActivity().getResources().getQuantityString(R.plurals.ConversationListFragment_this_will_permanently_delete_all_n_selected_conversations,
                                                                    conversationsCount, conversationsCount));
    alert.setCancelable(true);

    alert.setPositiveButton(R.string.delete, (dialog, which) -> {

      if (!deleteConversationSet.isEmpty()) {
        new AsyncTask<Void, Void, Void>() {
          private ProgressDialog dialog;

          @Override
          protected void onPreExecute() {
            dialog = ProgressDialog.show(getActivity(),
                    getActivity().getString(R.string.ConversationListFragment_deleting),
                    getActivity().getString(R.string.ConversationListFragment_deleting_selected_conversations),
                    true, false);
          }

          @Override
          protected Void doInBackground(Void... params) {
            SignalDatabase.threads().deleteConversations(deleteConversationSet);
            ApplicationDependencies.getMessageNotifier().updateNotification(getActivity());
            return null;
          }

          @Override
          protected void onPostExecute(Void result) {
            dialog.dismiss();
          }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
      }
    });

    alert.setNegativeButton(android.R.string.cancel, null);
    alert.show();
  }


  private void backToConversationListFragment() {
    if(getActivity() != null){
      getActivity().onBackPressed();
    }
  }

  @Override
  public boolean onBackPressed() {
    getNavigator().setFromOptions(true);
    return super.onBackPressed();
  }
}
