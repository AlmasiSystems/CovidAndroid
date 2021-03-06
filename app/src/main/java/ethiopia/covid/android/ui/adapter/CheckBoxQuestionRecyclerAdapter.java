package ethiopia.covid.android.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.List;

import ethiopia.covid.android.R;
import ethiopia.covid.android.data.QuestionItem;

/**
 * Created by BrookMG on 4/12/2020 in ethiopia.covid.android.ui.adapter
 * inside the project CoVidEt .
 */
public class CheckBoxQuestionRecyclerAdapter extends RecyclerView.Adapter<CheckBoxQuestionRecyclerAdapter.ViewHolder> {

    private List<QuestionItem> questionItems;
    private boolean singleCheckBoxOnly;
    private OnQuestionItemSelected onQuestionCheckBoxClicked;

    private int itemAlreadyChecked = -1;

    public CheckBoxQuestionRecyclerAdapter(List<QuestionItem> questionItems, OnQuestionItemSelected onQuestionCheckBoxClicked) {
        this(questionItems , false, onQuestionCheckBoxClicked);
    }

    public CheckBoxQuestionRecyclerAdapter(List<QuestionItem> questionItems, boolean singleCheckBoxOnly, OnQuestionItemSelected onQuestionCheckBoxClicked) {
        this.questionItems = questionItems;
        this.singleCheckBoxOnly = singleCheckBoxOnly;
        this.onQuestionCheckBoxClicked = onQuestionCheckBoxClicked;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.question_page_two_recycler_element , parent, false)
        );
    }

    private int numberOfSelectedItems() {
        int returnable = 0;

        for (QuestionItem item : questionItems)
            if (item.isSelectedQuestion())
                returnable++;

        return returnable;
    }

    private void singleSelectItem(ViewHolder holder, int position, boolean selection) {

        for (int i = 0; i < questionItems.size(); i++) {
            if (i == position) questionItems.get(position).setSelectedQuestion(selection);
            else questionItems.get(i).setSelectedQuestion(false);

            int finalI = i;
            holder.itemView.post(() -> notifyItemChanged(finalI));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.mainCheckBox.setText(questionItems.get(position).getQuestionText());
        holder.mainCheckBox.setOnCheckedChangeListener(null);
        holder.mainCheckBox.setChecked(questionItems.get(position).isSelectedQuestion());

        holder.mainCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (singleCheckBoxOnly) {
                singleSelectItem(holder, holder.getAdapterPosition(), isChecked);
            } else {
                questionItems.get(holder.getAdapterPosition()).setSelectedQuestion(isChecked);
            }

            if (onQuestionCheckBoxClicked != null) {
                onQuestionCheckBoxClicked.onItemSelected(
                        questionItems.get(holder.getAdapterPosition()),
                        holder.getAdapterPosition()
                );
            }
        });
    }

    @Override
    public int getItemCount() {
        return questionItems.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        MaterialCheckBox mainCheckBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mainCheckBox = itemView.findViewById(R.id.main_check_box);
        }
    }

}
