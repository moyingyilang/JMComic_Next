package com.jmcomic_next.lyqs;

import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.View;
import androidx.databinding.DataBinderMapper;
import androidx.databinding.DataBindingComponent;
import androidx.databinding.ViewDataBinding;
import com.jmcomic_next.lyqs.databinding.ActivityHomeBindingImpl;
import com.jmcomic_next.lyqs.databinding.ItemHomeBannerBindingImpl;
import com.jmcomic_next.lyqs.databinding.ItemHomeCategoryBindingImpl;
import com.jmcomic_next.lyqs.databinding.ItemHomeComicBindingImpl;
import java.lang.IllegalArgumentException;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.RuntimeException;
import java.lang.String;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DataBinderMapperImpl extends DataBinderMapper {
  private static final int LAYOUT_ACTIVITYHOME = 1;

  private static final int LAYOUT_ITEMHOMEBANNER = 2;

  private static final int LAYOUT_ITEMHOMECATEGORY = 3;

  private static final int LAYOUT_ITEMHOMECOMIC = 4;

  private static final SparseIntArray INTERNAL_LAYOUT_ID_LOOKUP = new SparseIntArray(4);

  static {
    INTERNAL_LAYOUT_ID_LOOKUP.put(com.jmcomic_next.lyqs.R.layout.activity_home, LAYOUT_ACTIVITYHOME);
    INTERNAL_LAYOUT_ID_LOOKUP.put(com.jmcomic_next.lyqs.R.layout.item_home_banner, LAYOUT_ITEMHOMEBANNER);
    INTERNAL_LAYOUT_ID_LOOKUP.put(com.jmcomic_next.lyqs.R.layout.item_home_category, LAYOUT_ITEMHOMECATEGORY);
    INTERNAL_LAYOUT_ID_LOOKUP.put(com.jmcomic_next.lyqs.R.layout.item_home_comic, LAYOUT_ITEMHOMECOMIC);
  }

  @Override
  public ViewDataBinding getDataBinder(DataBindingComponent component, View view, int layoutId) {
    int localizedLayoutId = INTERNAL_LAYOUT_ID_LOOKUP.get(layoutId);
    if(localizedLayoutId > 0) {
      final Object tag = view.getTag();
      if(tag == null) {
        throw new RuntimeException("view must have a tag");
      }
      switch(localizedLayoutId) {
        case  LAYOUT_ACTIVITYHOME: {
          if ("layout/activity_home_0".equals(tag)) {
            return new ActivityHomeBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for activity_home is invalid. Received: " + tag);
        }
        case  LAYOUT_ITEMHOMEBANNER: {
          if ("layout/item_home_banner_0".equals(tag)) {
            return new ItemHomeBannerBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for item_home_banner is invalid. Received: " + tag);
        }
        case  LAYOUT_ITEMHOMECATEGORY: {
          if ("layout/item_home_category_0".equals(tag)) {
            return new ItemHomeCategoryBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for item_home_category is invalid. Received: " + tag);
        }
        case  LAYOUT_ITEMHOMECOMIC: {
          if ("layout/item_home_comic_0".equals(tag)) {
            return new ItemHomeComicBindingImpl(component, view);
          }
          throw new IllegalArgumentException("The tag for item_home_comic is invalid. Received: " + tag);
        }
      }
    }
    return null;
  }

  @Override
  public ViewDataBinding getDataBinder(DataBindingComponent component, View[] views, int layoutId) {
    if(views == null || views.length == 0) {
      return null;
    }
    int localizedLayoutId = INTERNAL_LAYOUT_ID_LOOKUP.get(layoutId);
    if(localizedLayoutId > 0) {
      final Object tag = views[0].getTag();
      if(tag == null) {
        throw new RuntimeException("view must have a tag");
      }
      switch(localizedLayoutId) {
      }
    }
    return null;
  }

  @Override
  public int getLayoutId(String tag) {
    if (tag == null) {
      return 0;
    }
    Integer tmpVal = InnerLayoutIdLookup.sKeys.get(tag);
    return tmpVal == null ? 0 : tmpVal;
  }

  @Override
  public String convertBrIdToString(int localId) {
    String tmpVal = InnerBrLookup.sKeys.get(localId);
    return tmpVal;
  }

  @Override
  public List<DataBinderMapper> collectDependencies() {
    ArrayList<DataBinderMapper> result = new ArrayList<DataBinderMapper>(1);
    result.add(new androidx.databinding.library.baseAdapters.DataBinderMapperImpl());
    return result;
  }

  private static class InnerBrLookup {
    static final SparseArray<String> sKeys = new SparseArray<String>(4);

    static {
      sKeys.put(0, "_all");
      sKeys.put(1, "banner");
      sKeys.put(2, "category");
      sKeys.put(3, "comic");
    }
  }

  private static class InnerLayoutIdLookup {
    static final HashMap<String, Integer> sKeys = new HashMap<String, Integer>(4);

    static {
      sKeys.put("layout/activity_home_0", com.jmcomic_next.lyqs.R.layout.activity_home);
      sKeys.put("layout/item_home_banner_0", com.jmcomic_next.lyqs.R.layout.item_home_banner);
      sKeys.put("layout/item_home_category_0", com.jmcomic_next.lyqs.R.layout.item_home_category);
      sKeys.put("layout/item_home_comic_0", com.jmcomic_next.lyqs.R.layout.item_home_comic);
    }
  }
}
