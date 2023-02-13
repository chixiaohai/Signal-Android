package org.thoughtcrime.securesms.conversationlist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import org.signal.core.util.concurrent.SimpleTask;
import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.database.SignalDatabase;
import org.thoughtcrime.securesms.database.ThreadTable;
import org.thoughtcrime.securesms.database.model.ThreadRecord;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.search.MessageResult;
import org.thoughtcrime.securesms.search.SearchResult;
import org.thoughtcrime.securesms.util.ServiceUtil;
import org.thoughtcrime.securesms.util.StickyHeaderDecoration;

import java.util.Locale;

public class ConversationListSearchFragment extends ConversationListFragment
    implements ConversationListSearchAdapter.EventListener{

  private static final String TAG = Log.tag(ConversationListSearchFragment.class);

  private RecyclerView list;
  private ConversationListViewModel viewModel;
  private ConversationListSearchAdapter searchAdapter;
  private StickyHeaderDecoration searchAdapterDecoration;

  public static ConversationListSearchFragment newInstance() {
    return new ConversationListSearchFragment();
  }

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle bundle) {
    return inflater.inflate(R.layout.conversation_list_fragment, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    Log.i(TAG,"onViewCreated");
    list = view.findViewById(R.id.list);
    list.requestFocus();
    initializeViewModel();
    initializeListAdapters();
  }

  @Override
  public void onResume() {
    super.onResume();
    list.setAdapter(searchAdapter);
    list.getAdapter().notifyDataSetChanged();
  }

  private void initializeViewModel() {
    viewModel = new ViewModelProvider(this, new ConversationListViewModel.Factory(isArchived(), "")).get(ConversationListViewModel.class);
    viewModel.getSearchResult().observe(getViewLifecycleOwner(), result -> {
      result = result != null ? result : SearchResult.EMPTY;
      searchAdapter.updateResults(result);
    });
    viewModel.onSearchQueryUpdated("");
  }

  private void initializeListAdapters() {
    searchAdapter = new ConversationListSearchAdapter(requireContext(), GlideApp.with(this), this, Locale.getDefault());
    searchAdapterDecoration = new StickyHeaderDecoration(searchAdapter, false, false);
    list.setAdapter(searchAdapter);
  }

  @Override
  public void onConversationClicked(@NonNull ThreadRecord threadRecord) {
    hideKeyboard();
    getNavigator().goToConversation(threadRecord.getRecipient().getId(),
                                    threadRecord.getThreadId(),
                                    threadRecord.getDistributionType(),
                                    -1);
  }

  @Override
  public void onContactClicked(@NonNull Recipient contact) {
    SimpleTask.run(getViewLifecycleOwner().getLifecycle(), () -> {
      return SignalDatabase.threads().getThreadIdIfExistsFor(contact.getId());
    }, threadId -> {
      hideKeyboard();
      getNavigator().goToConversation(contact.getId(),
                                      threadId,
                                      ThreadTable.DistributionTypes.DEFAULT, -1);
    });
  }

  @Override
  public void onMessageClicked(MessageResult message) {
    SimpleTask.run(getViewLifecycleOwner().getLifecycle(), () -> {
      int startingPosition = SignalDatabase.mmsSms().getMessagePositionInConversation(message.getThreadId(), message.getReceivedTimestampMs());
      return Math.max(0, startingPosition);
    }, startingPosition -> {
      hideKeyboard();
      getNavigator().goToConversation(message.getConversationRecipient().getId(),
                                      message.getThreadId(),
                                      ThreadTable.DistributionTypes.DEFAULT,
                                      startingPosition);
    });
  }

  @Override
  public void onSearchTextChange(String text) {
    String trimmed = text.trim();
    viewModel.onSearchQueryUpdated(trimmed);
    //TODO Temp mark decoration to avoid bug, need further check.
    /*if (trimmed.length() > 0) {
      list.removeItemDecoration(searchAdapterDecoration);
      list.addItemDecoration(searchAdapterDecoration);
    } else {
      list.removeItemDecoration(searchAdapterDecoration);
    }*/
  }

  private void hideKeyboard() {
    InputMethodManager imm = ServiceUtil.getInputMethodManager(requireContext());
    imm.hideSoftInputFromWindow(requireView().getWindowToken(), 0);
  }

  protected boolean isArchived() {
    return false;
  }

  @Override
  public boolean onBackPressed() {
    getNavigator().setFromSearch(true);
    return super.onBackPressed();
  }
}
