/*
 * TongSheng TSDZ2 motor controller firmware/
 *
 * Copyright (C) Casainho, Leon, MSpider65 2020.
 *
 * Released under the GPL License, Version 3
 */

#ifndef _EBIKE_APP_H_
#define _EBIKE_APP_H_

#include <stdint.h>
#include "main.h"

// cadence sensor
extern uint16_t ui16_cadence_ticks_count_min_speed_adj;
extern volatile uint8_t ui8_brake_fast_stop;
extern volatile uint8_t ui8_adc_motor_phase_current_max;

// Torque sensor coaster brake engaged threshold value
extern uint16_t ui16_adc_coaster_brake_threshold;

typedef struct _configuration_variables {
    uint16_t ui16_battery_low_voltage_cut_off_x10;
    uint16_t ui16_wheel_perimeter;
    uint8_t ui8_wheel_speed_max;
    uint8_t ui8_foc_angle_multiplicator;
    uint8_t ui8_pedal_torque_per_10_bit_ADC_step_x100;
    uint8_t ui8_target_battery_max_power_div25;
    uint8_t ui8_optional_ADC_function;
    uint8_t ui8_torque_smooth_min;
    uint8_t ui8_torque_smooth_max;
    uint8_t ui8_torque_smooth_enabled;
} struct_configuration_variables;

void ebike_app_controller(void);
void new_torque_sample(void);

#endif /* _EBIKE_APP_H_ */
