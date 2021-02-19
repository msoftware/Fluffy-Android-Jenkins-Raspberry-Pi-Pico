import time, _thread
from machine import Pin, PWM
from time import sleep

led = Pin(25, Pin.OUT)
led.value(1)

pwm1 = PWM(Pin(13))
pwm1.freq(100)
pwm2 = PWM(Pin(14))
pwm2.freq(100)
pwm3 = PWM(Pin(15))
pwm3.freq(100)

def setPos (d1,d2,d3):
    pwm1.duty_u16(d1)
    pwm2.duty_u16(d2)
    pwm3.duty_u16(d3)
    sleep(0.05)
    pwm1.duty_u16(0)
    pwm2.duty_u16(0)
    pwm3.duty_u16(0)
    sleep(0.01)

def buttonTask():
    lastButtonValue = 1
    button = Pin (16, Pin.IN, Pin.PULL_UP)
    while True:
        buttonValue = button.value()
        if buttonValue != lastButtonValue:
            if buttonValue == 1:
                print ("BUTTON")
        lastButtonValue = buttonValue
        time.sleep(0.1)
    
sleep(1)
print ("START")
_thread.start_new_thread(buttonTask, ())
setPos (4000,8000,4500)
while True:
    input1 = input()
    values = input1.split(";")
    setPos (int(values[0]),int(values[1]),int(values[2]))
    print ("OK")
