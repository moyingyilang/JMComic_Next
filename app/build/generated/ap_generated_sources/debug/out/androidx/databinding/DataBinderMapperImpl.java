package androidx.databinding;

public class DataBinderMapperImpl extends MergedDataBinderMapper {
  DataBinderMapperImpl() {
    addMapper(new com.jmcomic_next.lyqs.DataBinderMapperImpl());
  }
}
