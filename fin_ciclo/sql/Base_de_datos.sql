/* 
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Other/SQLTemplate.sql to edit this template
 */
/**
 * Author:  Xoan Veiga
 * Created: 28 oct 2025
 */

CREATE DATABASE IF NOT EXISTS PersonasMudas;

CREATE TABLE gestos (
ID CHAR(2) PRIMARY KEY,
Imagen VARCHAR(100) NOT NULL,
Significado VARCHAR(40) NOT NULL 
)

CREATE TABLE gestosPersonas (
ID CHAR(2) PRIMARY KEY,
ImagenPersona VARCHAR(100) NOT NULL,
Fecha DATE NOT NULL,
Hora TIME NOT NULL,
GestoPersona CHAR(2) NOT NULL,
FOREIGN KEY (GestoPersona) REFERENCES gestos(ID)
)

