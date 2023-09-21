package com.code.weather

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.code.weather.databinding.ActivityMainBinding
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var workManager: WorkManager
    private lateinit var periodicWorkRequest: PeriodicWorkRequest
    private lateinit var binding: ActivityMainBinding


    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){isGranted:Boolean ->
        if(isGranted){
            Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
        }else{
            Toast.makeText(this, "Permission Declined", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        workManager = WorkManager.getInstance(this)
        binding.btnOneTimeTask.setOnClickListener(this)
        binding.btnPeriodicTask.setOnClickListener(this)
        binding.btnCancelTask.setOnClickListener(this)

        if(Build.VERSION.SDK_INT >= 33){
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onClick(v: View) {
        when(v.id){
            R.id.btnOneTimeTask -> startOneTimeTask()
            R.id.btnPeriodicTask-> startPeriodicTask()
            R.id.btnCancelTask -> cancelPeriodicTask()
        }
    }




    private fun startOneTimeTask() {
        binding.textstatus.text = getString(R.string.status)
        val data = Data.Builder()
            .putString(MyWorker.EXTRA_CITY, binding.edtCity.text.toString())
            .build()
        val oneTimeWorkRequest = OneTimeWorkRequest.Builder(MyWorker::class.java)
            .setInputData(data)
            .build()

        workManager.enqueue(oneTimeWorkRequest)
        workManager.getWorkInfoByIdLiveData(oneTimeWorkRequest.id)
            .observe(this){workInfo ->
                val status = workInfo.state.name
                binding.textstatus.append("\n" + status)
            }
    }
    private fun startPeriodicTask() {
        binding.textstatus.text = getString(R.string.status)
        val data = Data.Builder()
            .putString(MyWorker.EXTRA_CITY, binding.edtCity.text.toString())
            .build()
        val constraint = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        periodicWorkRequest = PeriodicWorkRequest.Builder(MyWorker::class.java,15,TimeUnit.MINUTES)
            .setInputData(data)
            .setConstraints(constraint)
            .build()
        workManager.enqueue(periodicWorkRequest)
        workManager.getWorkInfoByIdLiveData(periodicWorkRequest.id)
            .observe(this@MainActivity){workInfo->
                val status = workInfo.state.name
                binding.textstatus.append("\n" + status)
                binding.btnCancelTask.isEnabled = false
                if(workInfo.state==WorkInfo.State.ENQUEUED){
                    binding.btnCancelTask.isEnabled = true
                }
            }
    }

    private fun cancelPeriodicTask() {
        workManager.cancelWorkById(periodicWorkRequest.id)
    }
}