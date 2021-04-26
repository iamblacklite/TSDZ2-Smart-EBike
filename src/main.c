/*
 * TongSheng TSDZ2 motor controller firmware/
 *
 * Copyright (C) Casainho, Leon, MSpider65 2020.
 *
 * Released under the GPL License, Version 3
 */

#include <stdint.h>
#include <stdio.h>
#include "interrupts.h"
#include "stm8s.h"
#include "pins.h"
#include "uart.h"
#include "pwm.h"
#include "motor.h"
#include "wheel_speed_sensor.h"
#include "brake.h"
#include "pas.h"
#include "adc.h"
#include "timers.h"
#include "ebike_app.h"
#include "torque_sensor.h"
#include "lights.h"

/////////////////////////////////////////////////////////////////////////////////////////////
//// Functions prototypes

// main -- start of firmware and main loop
int main(void);

// With SDCC, interrupt service routine function prototypes must be placed in the file that contains main ()
// in order for an vector for the interrupt to be placed in the the interrupt vector space.  It's acceptable
// to place the function prototype in a header file as long as the header file is included in the file that
// contains main ().  SDCC will not generate any warnings or errors if this is not done, but the vector will
// not be in place so the ISR will not be executed when the interrupt occurs.

// Calling a function from interrupt not always works, SDCC manual says to avoid it. Maybe the best is to put
// all the code inside the interrupt

// Local VS global variables
// Sometimes I got the following error when compiling the firmware: motor.asm:750: Error: <r> relocation error
// when I have this code inside a function: "static uint8_t ui8_example_counter = 0;"
// and the solution was define the variable as global instead
// Another error example:
// *** buffer overflow detected ***: sdcc terminated
// Caught signal 6: SIGABRT

#ifdef __CDT_PARSER__
#define __interrupt(x)
#endif

// PWM cycle interrupt (called every 64us)
void TIM1_CAP_COM_IRQHandler(void) __interrupt(TIM1_CAP_COM_IRQHANDLER);
// UART Receive interrupt
void UART2_RX_IRQHandler(void) __interrupt(UART2_RX_IRQHANDLER);
// UART TX interrupt
void UART2_TX_IRQHandler(void) __interrupt(UART2_TX_IRQHANDLER);
// TIM4 Overflow interrupt (called every 1ms)
void TIM4_IRQHandler(void) __interrupt(TIM4_OVF_IRQHANDLER);
// Hall Sensor Signal interrupt
void HALL_SENSOR_A_PORT_IRQHandler(void) __interrupt(EXTI_HALL_A_IRQ);
void HALL_SENSOR_B_PORT_IRQHandler(void) __interrupt(EXTI_HALL_B_IRQ);
void HALL_SENSOR_C_PORT_IRQHandler(void) __interrupt(EXTI_HALL_C_IRQ);

/////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////

#ifdef MAIN_TIME_DEBUG
volatile uint8_t ui8_main_time;
uint8_t ui8_max_motor_time = 0;
uint8_t ui8_max_ebike_time = 0;
#endif

uint16_t ui8_motor_controller_counter = 0;
uint16_t ui8_ebike_controller_counter = 0;

int main(void) {
    
    // set clock at the max 16 MHz
    CLK_HSIPrescalerConfig(CLK_PRESCALER_HSIDIV1);

    brake_init();
    while (GPIO_ReadInputPin(BRAKE__PORT, BRAKE__PIN) == 0)
        ; // hold here while brake is pressed -- this is a protection for development
    adc_init();
    lights_init();
    uart2_init();
    timers_init();
    torque_sensor_init();
    pas_init();
    wheel_speed_sensor_init();
    pwm_init();
    hall_sensor_init();
    enableInterrupts();

    while (1) {
        // read timer counters - cannot read directly or priority doesn't work
        ui8_motor_controller_counter = ui8_tim4_motor_counter;
        ui8_ebike_controller_counter = ui8_tim4_ebike_counter;
        // motor controller - run every 4ms (TIM4 counter @ 2ms)
        if (ui8_motor_controller_counter > 2) {
            // reset counter
            ui8_tim4_motor_counter = 0;
            // run controller function
            motor_controller();
            continue; // give priority
        }
        // ebike controller - run every 50ms (TIM4 counter @ 2ms)
        if (ui8_ebike_controller_counter > 25) {
            // reset counter
            ui8_tim4_ebike_counter = 0;
            // run controller function
            ebike_app_controller();
        }
    }
}

