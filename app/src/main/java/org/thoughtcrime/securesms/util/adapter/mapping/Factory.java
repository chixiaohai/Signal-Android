package org.thoughtcrime.securesms.util.adapter.mapping;

import android.view.ViewGroup;

import androidx.annotation.NonNull;

public interface Factory<T extends MappingModel<T>> extends MappingAdapter.Factory<T> {
  @NonNull MappingViewHolder<T> createViewHolder(@NonNull ViewGroup parent);
}
