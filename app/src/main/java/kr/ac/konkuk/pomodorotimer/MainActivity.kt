package kr.ac.konkuk.pomodorotimer

import android.media.SoundPool
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.RequiresApi

class MainActivity : AppCompatActivity() {

    //뷰 바인딩은 좀 더 나중 파트에서
    private val remainMinutesTextView: TextView by lazy {
        findViewById(R.id.remainMinutesTextView)
    }

    private val remainSecondsTextView: TextView by lazy {
        findViewById(R.id.remainSecondsTextView)
    }

    private val seekBar: SeekBar by lazy {
        findViewById(R.id.seekBar)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private val soundPool = SoundPool.Builder().build()

    private var currentCountDownTimer: CountDownTimer? = null // 앱이 시작하자마자 타이머가 생기는 것이 아니기 때문에 초기값을 null로 선언

    private var tickingSoundId: Int? = null //타이머가 갱신되면서 나오는 소리
    private var bellSoundId: Int? = null //타이머 종료 사운드

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //xml에 백그라운드로 컬러를 주면
        //기존의 하얀색 윈도우에서 하얀색이 보였다가 빨간색이 나타나는 구조
        //이것을 보완하기 위해서는 아예 윈도우 백그라운드를 변경하는 것이 깔끔
        //해당 설정은 theme에서 item에 넣어주는 방법으로 구현가능

        bindViews()
        initSounds()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onResume() {
        super.onResume()
        soundPool.autoResume()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onPause() {
        super.onPause()

        //모든 사운드를 멈춤
        soundPool.autoPause()
    }

    //memoryLeak 방지
    //오디오나 미디어는 아무리 압축을 한다해도 cost가 큰 편이라 사용하지 않을 때는 항상 메모리에서 해제해줘야한다.
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onDestroy() {
        super.onDestroy()
        soundPool.release()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun initSounds() {
        //SoundPool
        //오디오파일을 메모리에 로드한다음에 비교적 빠르게 재생하도록 도와주는 클래스, 너무 긴 파일은 메모리에 로드하기 어려움으로 제한이 걸려있음
        //되도록이면 짧은 영상만 재생할 수 있게 제한되어있다.

        //this <- 현재 액티비티

        //파일을 메모리에 로드
        tickingSoundId = soundPool.load(this, R.raw.timer_ticking, 1)
        bellSoundId = soundPool.load(this, R.raw.timer_bell, 1)
    }


    private fun bindViews() {
        seekBar.setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {
                    //SeekBar의 세가지 콜백들
                    //SeekBar와 CountdownTimer를 연동
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        //분이 1의 자리일 경우 앞에 0을 채워줘야함

                        //아래 쪽에서 updateSeekBar를 통해 계속해서 업데이트를 해주고 있음
                        //이 때, updateSeekBar내부에서는 seekbar의 progress를 조작하게 됨
                        //이 때 onProgressChanged가 호출됨
                        //즉, 카운트다운이 시작되서 시크바가 변경이 되는데, 그게 다시 onProgressChanged를 호출하게 되어서
                        //remain time을 00초로 고정을 시키게 됨

                        //onProgressChanged가 fromUser에선지 실제 코드에선지 구분이 되지 않은 것을 막기위해 명시적으로 해줌
                        //코드 상에서가 아닌 사용자가 실제 건드렸을때만 업데이트를 해주는 방식으로 문제를 해결 
                        if (fromUser) {
                            updateRemainTime(progress * 60 * 1000L)
                        }
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                        //다시 타이머를 시작할때 기존의 타이머를 멈춰줘야 함

                        stopCountDown()
                    }

                    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                        //시크바에서 터치를 땠을때 카운트다운 시작

                        //seekBar.progress는 분을 나타내기때문에 밀리초로 변환

                        //seekBar가 null이면 바로 리턴
                        //kotlin은 특정값이 아닌 expression도 리턴 가능
                        seekBar ?: return //?: -> elvis operator 좌측 값이 null일때 우측값을 return(onStopTrackingTouch자체를)을 의미)

                        //0분에서 시작할 경우 tickSound가 나지않도록
                        if(seekBar.progress == 0) {
                            stopCountDown()
                        } else {
                            //카운트다운을 시작한다.
                            startCountDown()
                        }
                    }
                }
        )
    }

    private fun createCountDownTimer(initialMillis: Long) =
//        return CountDownTimer(initialMillis, 1000L)
    //추상클래스이기 때문에 구현해줘야할 메소드를 구현해야만 함
    // 메소드 불러오는 방법
            // 리턴까지 생략 (바로 반환)
            object : CountDownTimer(initialMillis, 1000L) {
                //
                override fun onTick(millisUntilFinished: Long) {
                    //매 1초마다 UI를 갱신
                    updateRemainTime(millisUntilFinished)
                    updateSeekBar(millisUntilFinished)
                }

                @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
                override fun onFinish() {
                    //카운트다운을 완료한다.
                    completeCountDown()
                }

            }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun startCountDown() {
        currentCountDownTimer = createCountDownTimer(seekBar.progress * 60 * 1000L) //생성

        currentCountDownTimer?.start() //시작 (?. -> optional이라고 한다.)

        //카운트다운이 시작됨과 동시에
        tickingSoundId?.let {
            //3~4초 정도 되는 사운드 트랙을 반복하여 재생
            //사운드는 특정앱이아닌 디바이스에서 가지고있는 사운드에 재생을 요청하는 것이기 때문에
            //앱을 종료해도 사운드가 계속 들리게됨
            //따라서 앱내에서 pause 요청이 필요
            soundPool.play(it, 1F, 1F, 0, -1, 1F)
        }
        //해당 변수가 nullable할 경우 null이 아닐 경우 해당 변수를 바로 함수안에 넣을 수 있게 하는 방식
        //NPE 방지 방법중 하나 자동으로 변환해줌 ㅇㅇㅇ
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun completeCountDown() {
        //0으로 모두 초기화
        updateRemainTime(0)
        updateSeekBar(0)

        //tick 소리 정지 후 벨소리
        soundPool.autoPause()
        bellSoundId?.let { soundPool.play(it, 1F, 1F, 0, 0, 1F) }
    }

    private fun updateRemainTime(remainMillis: Long) {
        val remainSeconds = remainMillis / 1000
        remainMinutesTextView.text = "%02d'".format(remainSeconds / 60)
        remainSecondsTextView.text = "%02d".format(remainSeconds % 60)
    }

    //인자는 똑같이 Milis로
    //받는 인자의 단위를 통일 시켜놓는 것이 가독성에 있어서 좋음(메소드내에서 처리가 까다롭더라도 ㅇㅇ)
    //가공에서 인자에 넣을 경우 사용할 때 오히려 헷갈릴수가 있음
    private fun updateSeekBar(remainMillis: Long) {
        seekBar.progress = (remainMillis / 1000 / 60).toInt()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun stopCountDown() {
        //현재 진행중인 타이머가 있나 확인
        currentCountDownTimer?.cancel() // null이 아닐 경우 cancel
        currentCountDownTimer = null
        soundPool.autoPause()
    }
}