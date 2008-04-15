package org.dodgybits.android.shuffle.view;

import org.dodgybits.android.shuffle.R;
import org.dodgybits.android.shuffle.model.Project;

import android.content.Context;
import android.util.SparseIntArray;
import android.view.ViewInflate;
import android.widget.TextView;

public class ProjectView extends ItemView<Project> {
	private TextView mName;
	private SparseIntArray mTaskCountArray;
	
	public ProjectView(Context androidContext) {
		super(androidContext);
		
        ViewInflate vi = (ViewInflate)androidContext.getSystemService(Context.INFLATE_SERVICE); 
        vi.inflate(getViewResourceId(), this, true, null); 

		mName = (TextView) findViewById(R.id.name);
	}
	
	protected int getViewResourceId() {
		return R.layout.list_item_view;
	}

	
	public void setTaskCountArray(SparseIntArray taskCountArray) {
		mTaskCountArray = taskCountArray;
	}
	
	@Override
	public void updateView(Project project, boolean isSelected) {
		if (mTaskCountArray != null) {
			Integer count = mTaskCountArray.get(project.id);
			if (count == null) count = 0;
			mName.setText(project.name + " (" + count + ")");
		} else {
			mName.setText(project.name);
		}
		
	}

}
