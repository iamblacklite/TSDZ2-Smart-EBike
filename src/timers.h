/*
 * TongSheng TSDZ2 motor controller firmware/
 *
 * Copyright (C) Casainho, 2018.
 *
 * Released under the GPL License, Version 3
 */

#ifndef _TIMERS_H_
#define _TIMERS_H_

// main loop TIM4 variable counters
extern volatile uint8_t ui8_tim4_motor_counter;
extern volatile uint8_t ui8_tim4_ebike_counter;

void timers_init(void);

#endif /* _TIMERS_H_ */
