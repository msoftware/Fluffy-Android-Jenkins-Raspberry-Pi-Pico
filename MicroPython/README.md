# Micropython Code 

The Code for the Raspberry PI Pico is in main.py. I have used the GPIOs 13,14,15 for the 3 Servos and 16 for the button.

## Install 

You can use Thonny (ihttps://thonny.org/) to save main.py on your Raspberry PI Pico.
Please note, the python code uses the second processor core for the _thread module. This can lead to problems with the Thonny IDE.
To avoid this problem, you can save the python file with a different name. In this case it is not started by default and you can just disconnect and connect the Raspberry PI Pico to continue development. But if you see the following Error you need to remove or rename main.py without using Thonny . 

Traceback (most recent call last):
  File "main.py", line 45, in <module>
ValueError: invalid syntax for integer with base 10
MicroPython v1.14 on 2021-02-05; Raspberry Pi Pico with RP2040
Type "help()" for more information.
ERROR   thonny.plugins.micropython.backend: Crash in backend
Traceback (most recent call last):
  File "/Applications/Thonny.app/Contents/Frameworks/Python.framework/Versions/3.7/lib/python3.7/site-packages/thonny/plugins/micropython/backend.py", line 136, in __init__
    self._prepare_after_soft_reboot(clean)
  File "/Applications/Thonny.app/Contents/Frameworks/Python.framework/Versions/3.7/lib/python3.7/site-packages/thonny/plugins/micropython/backend.py", line 155, in _prepare_after_soft_reboot
    self._execute_without_output(script)
  File "/Applications/Thonny.app/Contents/Frameworks/Python.framework/Versions/3.7/lib/python3.7/site-packages/thonny/plugins/micropython/backend.py", line 528, in _execute_without_output
    raise ManagementError(script, out, err)
thonny.plugins.micropython.backend.ManagementError: Problem with a management command


## How to delete or rename files from Raspberry Pi Pico with MicroPython 

Install screen on your computer (Mac or Linux). On Windowes you can use Putty.

Execute the following command to connect to your Raspberry Pi Pico (The device name may differ from cu.usbmodem0000000000001)

screen /dev/cu.usbmodem0000000000001

Now you are connected to a MicroPython environment on your Raspberry Pi Pico. You can execute all python commands (See https://micropython.org/ for details)

If you see a blank screen, just press enter and you will see:

>>>

You can call "help()" and you will get the following output.

>>> help()
Welcome to MicroPython!

For online help please visit https://micropython.org/help/.

For access to the hardware use the 'machine' module.  RP2 specific commands
are in the 'rp2' module.

Quick overview of some objects:
  machine.Pin(pin) -- get a pin, eg machine.Pin(0)
  machine.Pin(pin, m, [p]) -- get a pin and configure it for IO mode m, pull mode p
    methods: init(..), value([v]), high(), low(), irq(handler)
  machine.ADC(pin) -- make an analog object from a pin
    methods: read_u16()
  machine.PWM(pin) -- make a PWM object from a pin
    methods: deinit(), freq([f]), duty_u16([d]), duty_ns([d])
  machine.I2C(id) -- create an I2C object (id=0,1)
    methods: readfrom(addr, buf, stop=True), writeto(addr, buf, stop=True)
             readfrom_mem(addr, memaddr, arg), writeto_mem(addr, memaddr, arg)
  machine.SPI(id, baudrate=1000000) -- create an SPI object (id=0,1)
    methods: read(nbytes, write=0x00), write(buf), write_readinto(wr_buf, rd_buf)
  machine.Timer(freq, callback) -- create a software timer object
    eg: machine.Timer(freq=1, callback=lambda t:print(t))

Pins are numbered 0-29, and 26-29 have ADC capabilities
Pin IO modes are: Pin.IN, Pin.OUT, Pin.ALT
Pin pull modes are: Pin.PULL_UP, Pin.PULL_DOWN

Useful control commands:
  CTRL-C -- interrupt a running program
  CTRL-D -- on a blank line, do a soft reset of the board
  CTRL-E -- on a blank line, enter paste mode

For further help on a specific object, type help(obj)
For a list of available modules, type help('modules')


Now you are in the MicroPython env. and you can use python methods to do something on the filesystem.

>>> 
>>> import os
>>> os.listdir()
['blink.py', 'button.py', 'main.py']
>>> os.rename('main.py','test.py')
>>> os.listdir()
['blink.py', 'button.py', 'test.py']
>>> 


You can now quit the screen session by "ctrl + a" "k" and press "y".


