package cmu.cconfs.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import cmu.cconfs.R;
import cmu.cconfs.model.parseModel.Paper;

/**
 * Created by zmhbh on 8/17/15.
 */
public class PaperListViewAdapter extends BaseAdapter {

    private LayoutInflater adapterLayoutInflater;
    private List<Paper> papers;


    public PaperListViewAdapter(Context c, List<Paper> papers) {
        adapterLayoutInflater = LayoutInflater.from(c);
        this.papers = papers;
    }

    @Override
    public int getCount() {
        if (papers == null)
            return 0;
        return papers.size();
    }

    @Override
    public Object getItem(int position) {
        if (papers != null) {
            return papers.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final Paper paper = papers.get(position);
        View view = adapterLayoutInflater.inflate(R.layout.session_paper_cardview, parent, false);

        TextView paperTitleTextView = (TextView) view.findViewById(R.id.paperTitle);
        TextView paperAffiTextView = (TextView) view.findViewById(R.id.paperAffiliation);
        paperTitleTextView.setText(paper.getTitle());
        paperAffiTextView.setText(paper.getAffiliation());

        return view;
    }
}
