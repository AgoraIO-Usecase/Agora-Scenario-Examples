package io.agora.sample.breakoutroom.ui.list;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import io.agora.sample.breakoutroom.bean.SceneInfo;

public class ListViewModel extends ViewModel {
    private final MutableLiveData<List<SceneInfo>> _sceneInfoList = new MutableLiveData<>();

    public ListViewModel() {

    }

    public LiveData<List<SceneInfo>> sceneInfoList(){
        return _sceneInfoList;
    }

    public void fetchData(){

    }
}
