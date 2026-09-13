package com.TechHurts.CodeFile;

import android.text.*;
import android.widget.*;
import io.github.rosemoe.sora.event.*;
import io.github.rosemoe.sora.widget.*;

/** Binds native search controls to asynchronous editor results; owns its subscriptions. */
final class NativeSearch implements AutoCloseable {
  private final CodeEditor editor;
  private boolean searchSelection;
  private final EditorSearcher searcher;
  private final EditText query;
  private final CheckBox matchCase, regex;
  private final TextView summary;
  private final SubscriptionReceipt<PublishSearchResultEvent> results;
  private final SubscriptionReceipt<SelectionChangeEvent> selection;
  private final Runnable pending = this::refresh;
  private final TextWatcher watcher =
      new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        public void onTextChanged(CharSequence s, int start, int before, int count) {
          query.removeCallbacks(pending);
          query.postDelayed(pending, 120);
        }

        public void afterTextChanged(Editable value) {}
      };

  NativeSearch(
      CodeEditor editor, EditText query, CheckBox matchCase, CheckBox regex, TextView summary) {
    this.editor = editor;
    this.searcher = editor.getSearcher();
    this.query = query;
    this.matchCase = matchCase;
    this.regex = regex;
    this.summary = summary;
    searcher.setCyclicJumping(true);
    results =
        editor.subscribeEvent(
            PublishSearchResultEvent.class, (event, unsubscribe) -> updateSummary());
    selection =
        editor.subscribeEvent(
            SelectionChangeEvent.class,
            (event, unsubscribe) -> {
              searchSelection = event.getCause() == SelectionChangeEvent.CAUSE_SEARCH;
              updateSummary();
            });
    query.addTextChangedListener(watcher);
    matchCase.setOnCheckedChangeListener((button, value) -> refresh());
    regex.setOnCheckedChangeListener((button, value) -> refresh());
    query.setSingleLine(true);
    query.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
    query.setOnEditorActionListener(
        (v, action, event) -> {
          next();
          return true;
        });
    refresh();
  }

  private void refresh() {
    query.removeCallbacks(pending);
    String term = query.getText().toString();
    if (term.isEmpty()) {
      searcher.stopSearch();
      summary.setText("Enter a search term");
      query.setError(null);
      return;
    }
    try {
      searcher.search(
          term, new EditorSearcher.SearchOptions(!matchCase.isChecked(), regex.isChecked()));
      query.setError(null);
      summary.setText("Searching…");
    } catch (java.util.regex.PatternSyntaxException invalid) {
      searcher.stopSearch();
      query.setError("Invalid regular expression");
      summary.setText("Check the regular expression");
    }
  }

  private void updateSummary() {
    if (!searcher.hasQuery()) {
      summary.setText("Enter a search term");
      return;
    }
    int total = searcher.getMatchedPositionCount(),
        current = searcher.getCurrentMatchedPositionIndex();
    summary.setText(
        total == 0
            ? "No matches"
            : current < 0 ? total + " matches" : (current + 1) + " / " + total);
  }

  void next() {
    if (searcher.hasQuery()) searcher.gotoNext();
    updateSummary();
  }

  void previous() {
    if (searcher.hasQuery()) searcher.gotoPrevious();
    updateSummary();
  }

  void applyHighlights(TextHighlights annotations, int color) {
    String term = query.getText().toString();
    if (term.isEmpty()) throw new IllegalArgumentException("Enter a search term first");
    int flags = java.util.regex.Pattern.MULTILINE;
    if (!matchCase.isChecked()) flags |= java.util.regex.Pattern.CASE_INSENSITIVE;
    String expression = regex.isChecked() ? term : java.util.regex.Pattern.quote(term);
    java.util.regex.Matcher matches =
        java.util.regex.Pattern.compile(expression, flags).matcher(editor.getText().toString());
    while (matches.find()) {
      if (matches.end() > matches.start()) annotations.apply(matches.start(), matches.end(), color);
    }
  }

  void clear() {
    boolean clearMatchSelection = searchSelection && editor.getCursor().isSelected();
    query.setText("");
    refresh();
    if (clearMatchSelection)
      editor.setSelection(editor.getCursor().getLeftLine(), editor.getCursor().getLeftColumn());
  }

  public void close() {
    query.removeCallbacks(pending);
    query.removeTextChangedListener(watcher);
    results.unsubscribe();
    selection.unsubscribe();
  }
}
