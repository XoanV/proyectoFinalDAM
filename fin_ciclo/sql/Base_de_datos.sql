/* 
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Other/SQLTemplate.sql to edit this template
 */
/**
 * Author:  Xoan Veiga
 * Created: 28 oct 2025
 */
drop database if exists PersonasMudas;
CREATE DATABASE PersonasMudas;

USE PersonasMudas;

CREATE TABLE gestos (
ID INT AUTO_INCREMENT PRIMARY KEY,
Imagen LONGBLOB NOT NULL,
Significado VARCHAR(40) NOT NULL 
);

CREATE TABLE gestosPersonas (
ID INT AUTO_INCREMENT PRIMARY KEY,
ImagenPersona LONGBLOB NOT NULL,
Fecha DATE NOT NULL,
Hora TIME NOT NULL,
ID_Gesto INT NOT NULL,
FOREIGN KEY (ID_Gesto) REFERENCES gestos(ID)
);
 
INSERT INTO gestos VALUES 
(1, LOAD_FILE( 'C:\\ProgramData\\MySQL\\MySQL Server 8.0\\Uploads\\adios.JPG'), "Adiós"), /*Hay que poner en esta ruta las imágenes para que la base datos pueda insertarlas*/
(2, LOAD_FILE( 'C:\\ProgramData\\MySQL\\MySQL Server 8.0\\Uploads\\noches.JPG'), "Buenas noches"),
(3, LOAD_FILE( 'C:\\ProgramData\\MySQL\\MySQL Server 8.0\\Uploads\\saludo.JPG'), "Hola");