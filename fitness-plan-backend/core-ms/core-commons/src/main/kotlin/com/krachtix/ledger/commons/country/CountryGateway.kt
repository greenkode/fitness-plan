package com.krachtix.commons.country


interface CountryGateway {

    fun findAllByEnabled(): List<Country>;
}