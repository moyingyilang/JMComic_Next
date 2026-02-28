package com.jmcomic_next.lyqs.databinding;
import com.jmcomic_next.lyqs.R;
import com.jmcomic_next.lyqs.BR;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
@SuppressWarnings("unchecked")
public class ItemHomeComicBindingImpl extends ItemHomeComicBinding  {

    @Nullable
    private static final androidx.databinding.ViewDataBinding.IncludedLayouts sIncludes;
    @Nullable
    private static final android.util.SparseIntArray sViewsWithIds;
    static {
        sIncludes = null;
        sViewsWithIds = new android.util.SparseIntArray();
        sViewsWithIds.put(R.id.iv_comic_cover, 4);
    }
    // views
    @NonNull
    private final androidx.cardview.widget.CardView mboundView0;
    // variables
    // values
    // listeners
    // Inverse Binding Event Handlers

    public ItemHomeComicBindingImpl(@Nullable androidx.databinding.DataBindingComponent bindingComponent, @NonNull View root) {
        this(bindingComponent, root, mapBindings(bindingComponent, root, 5, sIncludes, sViewsWithIds));
    }
    private ItemHomeComicBindingImpl(androidx.databinding.DataBindingComponent bindingComponent, View root, Object[] bindings) {
        super(bindingComponent, root, 0
            , (android.widget.ImageView) bindings[4]
            , (android.widget.TextView) bindings[2]
            , (android.widget.TextView) bindings[1]
            , (android.widget.TextView) bindings[3]
            );
        this.mboundView0 = (androidx.cardview.widget.CardView) bindings[0];
        this.mboundView0.setTag(null);
        this.tvAuthor.setTag(null);
        this.tvComicName.setTag(null);
        this.tvUpdateTime.setTag(null);
        setRootTag(root);
        // listeners
        invalidateAll();
    }

    @Override
    public void invalidateAll() {
        synchronized(this) {
                mDirtyFlags = 0x2L;
        }
        requestRebind();
    }

    @Override
    public boolean hasPendingBindings() {
        synchronized(this) {
            if (mDirtyFlags != 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean setVariable(int variableId, @Nullable Object variable)  {
        boolean variableSet = true;
        if (BR.comic == variableId) {
            setComic((com.jmcomic_next.lyqs.bean.ComicBean) variable);
        }
        else {
            variableSet = false;
        }
            return variableSet;
    }

    public void setComic(@Nullable com.jmcomic_next.lyqs.bean.ComicBean Comic) {
        this.mComic = Comic;
        synchronized(this) {
            mDirtyFlags |= 0x1L;
        }
        notifyPropertyChanged(BR.comic);
        super.requestRebind();
    }

    @Override
    protected boolean onFieldChange(int localFieldId, Object object, int fieldId) {
        switch (localFieldId) {
        }
        return false;
    }

    @Override
    protected void executeBindings() {
        long dirtyFlags = 0;
        synchronized(this) {
            dirtyFlags = mDirtyFlags;
            mDirtyFlags = 0;
        }
        java.lang.String comicAuthor = null;
        com.jmcomic_next.lyqs.bean.ComicBean comic = mComic;
        java.lang.String comicName = null;
        java.lang.String comicUpdateTime = null;

        if ((dirtyFlags & 0x3L) != 0) {



                if (comic != null) {
                    // read comic.author
                    comicAuthor = comic.getAuthor();
                    // read comic.name
                    comicName = comic.getName();
                    // read comic.updateTime
                    comicUpdateTime = comic.getUpdateTime();
                }
        }
        // batch finished
        if ((dirtyFlags & 0x3L) != 0) {
            // api target 1

            androidx.databinding.adapters.TextViewBindingAdapter.setText(this.tvAuthor, comicAuthor);
            androidx.databinding.adapters.TextViewBindingAdapter.setText(this.tvComicName, comicName);
            androidx.databinding.adapters.TextViewBindingAdapter.setText(this.tvUpdateTime, comicUpdateTime);
        }
    }
    // Listener Stub Implementations
    // callback impls
    // dirty flag
    private  long mDirtyFlags = 0xffffffffffffffffL;
    /* flag mapping
        flag 0 (0x1L): comic
        flag 1 (0x2L): null
    flag mapping end*/
    //end
}